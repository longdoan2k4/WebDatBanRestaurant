package com.example.restaurantpro.service;

import com.example.restaurantpro.config.VnPayProperties;
import com.example.restaurantpro.dto.VnPayCallbackResult;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentMethod;
import com.example.restaurantpro.model.PaymentStatus;
import com.example.restaurantpro.model.PaymentTransaction;
import com.example.restaurantpro.model.PaymentTransactionType;
import com.example.restaurantpro.repository.PaymentTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional
public class VnPayService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BigDecimal VNPAY_MINIMUM_AMOUNT = new BigDecimal("5000");

    private final VnPayProperties vnPayProperties;
    private final BookingService bookingService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public VnPayService(VnPayProperties vnPayProperties,
                        BookingService bookingService,
                        PaymentTransactionRepository paymentTransactionRepository) {
        this.vnPayProperties = vnPayProperties;
        this.bookingService = bookingService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public boolean isConfigured() {
        return vnPayProperties.isReady();
    }

    public String createPaymentUrl(Booking booking, HttpServletRequest request) {
        if (!vnPayProperties.isReady()) {
            throw new IllegalStateException("VNPAY chua duoc cau hinh. Hay cap nhat payment.vnpay trong application.yml.");
        }
        if (!booking.hasPayableAmount()) {
            throw new IllegalArgumentException("Booking khong co gia tri thanh toan qua VNPAY.");
        }
        if (booking.getFinalAmount().compareTo(VNPAY_MINIMUM_AMOUNT) < 0) {
            throw new IllegalArgumentException("So tien toi thieu cua VNPAY la 5.000 VND.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Booking nay da duoc thanh toan truoc do.");
        }
        if (booking.getPaymentStatus() == PaymentStatus.REFUND_PENDING || booking.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Booking dang o trang thai hoan tien, khong the tao giao dich moi.");
        }

        String txnRef = String.valueOf(booking.getId());
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByTxnRef(txnRef)
                .orElseGet(() -> new PaymentTransaction(booking, txnRef, booking.getFinalAmount()));
        paymentTransaction.setBooking(booking);
        paymentTransaction.setAmount(booking.getFinalAmount());
        paymentTransaction.setProvider("VNPAY");
        paymentTransaction.setType(PaymentTransactionType.PAYMENT);
        paymentTransaction.setStatus(PaymentStatus.PENDING);
        paymentTransaction.setMessage("Khoi tao giao dich VNPAY");
        paymentTransactionRepository.save(paymentTransaction);
        bookingService.markPaymentPending(booking, txnRef);

        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", sanitizeOrderInfo("Thanh toan booking " + booking.getId()));
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_Locale", vnPayProperties.getLocale());
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_IpAddr", resolveClientIp(request));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(vnPayProperties.getExpireMinutes()).format(VNPAY_DATE_FORMAT));

        String queryString = buildQueryString(params);
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        return vnPayProperties.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    public VnPayCallbackResult handleReturn(Map<String, String> responseParams) {
        return processCallback(responseParams);
    }

    public Map<String, String> handleIpn(Map<String, String> responseParams) {
        if (responseParams == null || responseParams.isEmpty()) {
            return Map.of("RspCode", "99", "Message", "Input data required");
        }
        if (!isValidSignature(responseParams)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String txnRef = responseParams.get("vnp_TxnRef");
        PaymentTransaction transaction = paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
        if (transaction == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }
        if (!isAmountValid(transaction, responseParams.get("vnp_Amount"))) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }
        if (transaction.getStatus() != null && transaction.getStatus() != PaymentStatus.PENDING) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        processCallback(responseParams);
        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    public String refundBooking(Booking booking, String createdBy, HttpServletRequest request) {
        if (!vnPayProperties.isReady()) {
            throw new IllegalStateException("VNPAY chua duoc cau hinh de thuc hien hoan tien.");
        }
        if (booking.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new IllegalStateException("Chi booking thanh toan bang VNPAY moi co the hoan tien online.");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Chi booking da thanh toan moi co the hoan tien.");
        }

        PaymentTransaction paidTransaction = paymentTransactionRepository
                .findFirstByBookingIdAndTypeOrderByCreatedAtDesc(booking.getId(), PaymentTransactionType.PAYMENT)
                .orElseThrow(() -> new IllegalStateException("Khong tim thay giao dich thanh toan goc de hoan tien."));

        if (paidTransaction.getTransactionNo() == null || paidTransaction.getTransactionNo().isBlank()) {
            throw new IllegalStateException("Chua co ma giao dich VNPAY, khong the gui lenh refund.");
        }
        if (paymentTransactionRepository.existsByOriginalTxnRefAndType(paidTransaction.getTxnRef(), PaymentTransactionType.REFUND)) {
            throw new IllegalStateException("Booking nay da ton tai yeu cau hoan tien truoc do.");
        }

        bookingService.markRefundPending(booking);

        String refundTxnRef = generateRefundTxnRef(booking.getId());
        booking.setLatestPaymentTxnRef(refundTxnRef);
        bookingService.save(booking);
        PaymentTransaction refundTransaction = new PaymentTransaction(booking, refundTxnRef, booking.getTotalAmount());
        refundTransaction.setProvider("VNPAY");
        refundTransaction.setType(PaymentTransactionType.REFUND);
        refundTransaction.setOriginalTxnRef(paidTransaction.getTxnRef());
        refundTransaction.setCreatedBy(createdBy);
        refundTransaction.setStatus(PaymentStatus.PENDING);
        refundTransaction.setMessage("Khoi tao yeu cau hoan tien VNPAY cho booking #" + booking.getId());
        paymentTransactionRepository.save(refundTransaction);

        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String transactionType = "02";
        String orderInfo = sanitizeOrderInfo("Hoan tien booking " + booking.getId());
        String ipAddr = resolveClientIp(request);
        String transactionDate = formatDateTime(paidTransaction.getCreatedAt());
        String createDate = now.format(VNPAY_DATE_FORMAT);
        String amount = booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();

        String hashData = String.join("|",
                requestId,
                "2.1.0",
                "refund",
                vnPayProperties.getTmnCode(),
                transactionType,
                paidTransaction.getTxnRef(),
                amount,
                paidTransaction.getTransactionNo(),
                transactionDate,
                createdBy == null ? "admin" : createdBy,
                createDate,
                ipAddr,
                orderInfo);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("vnp_RequestId", requestId);
        payload.put("vnp_Version", "2.1.0");
        payload.put("vnp_Command", "refund");
        payload.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        payload.put("vnp_TransactionType", transactionType);
        payload.put("vnp_TxnRef", paidTransaction.getTxnRef());
        payload.put("vnp_Amount", amount);
        payload.put("vnp_OrderInfo", orderInfo);
        payload.put("vnp_TransactionNo", paidTransaction.getTransactionNo());
        payload.put("vnp_TransactionDate", transactionDate);
        payload.put("vnp_CreateBy", createdBy == null ? "admin" : createdBy);
        payload.put("vnp_CreateDate", createDate);
        payload.put("vnp_IpAddr", ipAddr);
        payload.put("vnp_SecureHash", hmacSHA512(vnPayProperties.getHashSecret(), hashData));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    vnPayProperties.getRefundApiUrl(),
                    new HttpEntity<>(payload, headers),
                    Map.class);

            if (response == null) {
                throw new IllegalStateException("VNPAY khong tra ve du lieu refund.");
            }

            String responseCode = readString(response.get("vnp_ResponseCode"));
            String message = readString(response.get("vnp_Message"));
            String transactionStatus = readString(response.get("vnp_TransactionStatus"));
            String refundTransactionNo = readString(response.get("vnp_TransactionNo"));
            String bankCode = readString(response.get("vnp_BankCode"));

            refundTransaction.setResponseCode(responseCode);
            refundTransaction.setTransactionStatus(transactionStatus);
            refundTransaction.setTransactionNo(refundTransactionNo);
            refundTransaction.setBankCode(bankCode);
            refundTransaction.setMessage(message == null || message.isBlank() ? "Yeu cau refund da duoc gui toi VNPAY." : message);

            boolean accepted = "00".equals(responseCode);
            boolean pending = "05".equals(transactionStatus) || "06".equals(transactionStatus) || "94".equals(responseCode);

            if (accepted || pending) {
                refundTransaction.setStatus(accepted ? PaymentStatus.REFUNDED : PaymentStatus.REFUND_PENDING);
                refundTransaction.setRefundedAt(LocalDateTime.now(VN_ZONE));
                if (accepted) {
                    bookingService.markRefunded(booking, refundTransaction.getRefundedAt());
                } else {
                    booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                    bookingService.save(booking);
                }
            } else {
                refundTransaction.setStatus(PaymentStatus.FAILED);
                booking.setPaymentStatus(PaymentStatus.PAID);
                bookingService.save(booking);
            }
            paymentTransactionRepository.save(refundTransaction);

            return refundTransaction.getMessage();
        } catch (Exception ex) {
            refundTransaction.setStatus(PaymentStatus.FAILED);
            refundTransaction.setMessage("Gui yeu cau hoan tien that bai: " + ex.getMessage());
            paymentTransactionRepository.save(refundTransaction);
            booking.setPaymentStatus(PaymentStatus.PAID);
            bookingService.save(booking);
            throw new IllegalStateException(refundTransaction.getMessage(), ex);
        }
    }

    private VnPayCallbackResult processCallback(Map<String, String> responseParams) {
        if (responseParams == null || responseParams.isEmpty()) {
            return new VnPayCallbackResult(false, false, "Khong nhan duoc du lieu tra ve tu VNPAY.", null, null);
        }

        if (!isValidSignature(responseParams)) {
            return new VnPayCallbackResult(false, false, "Chu ky VNPAY khong hop le.", null, null);
        }

        String txnRef = responseParams.get("vnp_TxnRef");
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
        if (paymentTransaction == null) {
            return new VnPayCallbackResult(true, false, "Khong tim thay giao dich VNPAY tu he thong nha hang.", null, null);
        }
        if (!isAmountValid(paymentTransaction, responseParams.get("vnp_Amount"))) {
            return new VnPayCallbackResult(true, false, "So tien tra ve khong khop voi booking.", paymentTransaction.getBooking(), paymentTransaction);
        }

        Booking booking = paymentTransaction.getBooking();
        boolean successful = isSuccessfulResponse(responseParams);

        paymentTransaction.setResponseCode(responseParams.get("vnp_ResponseCode"));
        paymentTransaction.setTransactionStatus(responseParams.get("vnp_TransactionStatus"));
        paymentTransaction.setTransactionNo(responseParams.get("vnp_TransactionNo"));
        paymentTransaction.setBankCode(responseParams.get("vnp_BankCode"));
        paymentTransaction.setMessage(resolveResponseMessage(responseParams.get("vnp_ResponseCode")));

        if (paymentTransaction.getStatus() == PaymentStatus.PENDING) {
            if (successful) {
                paymentTransaction.setStatus(PaymentStatus.PAID);
                paymentTransaction.setPaidAt(parsePayDate(responseParams.get("vnp_PayDate")));
                bookingService.markPaid(booking, txnRef, paymentTransaction.getPaidAt());
            } else {
                paymentTransaction.setStatus(PaymentStatus.FAILED);
                bookingService.markPaymentFailed(booking, txnRef);
            }
            paymentTransactionRepository.save(paymentTransaction);
        }

        String message = successful
                ? "Booking #" + booking.getId() + " da duoc xac nhan thanh toan thanh cong. Nha hang da ghi nhan don dat ban cua ban."
                : resolveResponseMessage(responseParams.get("vnp_ResponseCode"));

        return new VnPayCallbackResult(true, successful, message, booking, paymentTransaction);
    }

    private boolean isSuccessfulResponse(Map<String, String> responseParams) {
        String responseCode = responseParams.get("vnp_ResponseCode");
        String transactionStatus = responseParams.get("vnp_TransactionStatus");
        return "00".equals(responseCode) && (transactionStatus == null || transactionStatus.isBlank() || "00".equals(transactionStatus));
    }

    private boolean isValidSignature(Map<String, String> responseParams) {
        String secureHash = responseParams.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        Map<String, String> filtered = new TreeMap<>();
        responseParams.forEach((key, value) -> {
            if (value != null && !value.isBlank() && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)) {
                filtered.put(key, value);
            }
        });
        String signValue = hmacSHA512(vnPayProperties.getHashSecret(), buildHashData(filtered));
        return secureHash.equalsIgnoreCase(signValue);
    }

    private boolean isAmountValid(PaymentTransaction paymentTransaction, String returnedAmount) {
        if (returnedAmount == null || returnedAmount.isBlank()) {
            return false;
        }
        String expectedAmount = paymentTransaction.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .toBigInteger()
                .toString();
        return expectedAmount.equals(returnedAmount);
    }

    private String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "127.0.0.1" : remoteAddr;
    }

    private String sanitizeOrderInfo(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private String generateRefundTxnRef(Long bookingId) {
        String timestamp = LocalDateTime.now(VN_ZONE).format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "RF" + bookingId + timestamp + random;
    }

    private LocalDateTime parsePayDate(String payDate) {
        if (payDate == null || payDate.isBlank()) {
            return LocalDateTime.now(VN_ZONE);
        }
        try {
            return LocalDateTime.parse(payDate, VNPAY_DATE_FORMAT);
        } catch (Exception ignored) {
            return LocalDateTime.now(VN_ZONE);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        LocalDateTime safeValue = value == null ? LocalDateTime.now(VN_ZONE) : value;
        return safeValue.format(VNPAY_DATE_FORMAT);
    }

    private String readString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public String resolveResponseMessage(String responseCode) {
        if (responseCode == null || responseCode.isBlank()) {
            return "Khong xac dinh duoc ket qua giao dich.";
        }
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("00", "Giao dich thanh cong.");
        messages.put("07", "Tien da bi tru nhung giao dich dang bi nghi ngo can kiem tra them.");
        messages.put("09", "Tai khoan hoac the chua dang ky Internet Banking.");
        messages.put("10", "Xac thuc thong tin khong dung qua 3 lan.");
        messages.put("11", "Giao dich da het han thanh toan.");
        messages.put("12", "The hoac tai khoan da bi khoa.");
        messages.put("13", "Sai mat khau xac thuc OTP.");
        messages.put("24", "Khach hang da huy giao dich.");
        messages.put("51", "Tai khoan khong du so du de thanh toan.");
        messages.put("65", "Tai khoan da vuot qua han muc giao dich trong ngay.");
        messages.put("75", "Ngan hang thanh toan dang bao tri.");
        messages.put("79", "Khach hang nhap sai mat khau thanh toan qua so lan quy dinh.");
        messages.put("91", "Khong tim thay giao dich de xu ly tren he thong VNPAY.");
        messages.put("94", "Yeu cau dang bi trung lap hoac refund dang duoc xu ly.");
        messages.put("95", "Giao dich goc khong hop le de hoan tien.");
        messages.put("97", "Checksum VNPAY khong hop le.");
        return messages.getOrDefault(responseCode, "Giao dich khong thanh cong. Ma phan hoi: " + responseCode);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                hash.append(String.format(Locale.ROOT, "%02x", aByte));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Khong the tao chu ky VNPAY.", e);
        }
    }
}
