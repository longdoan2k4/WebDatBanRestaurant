package com.example.restaurantpro.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VNPayConfig {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private VNPayConfig() {
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hash.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate VNPAY checksum", e);
        }
    }

    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!hashData.isEmpty()) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=').append(urlEncode(fieldValue));
            }
        }
        return hashData.toString();
    }

    public static String buildQueryUrl(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!query.isEmpty()) {
                    query.append('&');
                }
                query.append(urlEncode(fieldName)).append('=').append(urlEncode(fieldValue));
            }
        }
        return query.toString();
    }

    public static String randomTxnRef() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10000000L, 99999999L));
    }

    public static String nowVnPayFormat() {
        return LocalDateTime.now(VN_ZONE).format(VNPAY_DATE_FORMAT);
    }

    public static String plusMinutesVnPayFormat(int minutes) {
        return LocalDateTime.now(VN_ZONE).plusMinutes(minutes).format(VNPAY_DATE_FORMAT);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
