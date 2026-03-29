package com.example.restaurantpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnPayProperties {

    private boolean enabled = false;
    private String tmnCode;
    private String hashSecret;
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String refundApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl = "http://localhost:8089/payment/vnpay-return";
    private String orderType = "other";
    private String locale = "vn";
    private int expireMinutes = 15;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public void setTmnCode(String tmnCode) {
        this.tmnCode = tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getRefundApiUrl() {
        return refundApiUrl;
    }

    public void setRefundApiUrl(String refundApiUrl) {
        this.refundApiUrl = refundApiUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public boolean isReady() {
        return enabled
                && tmnCode != null && !tmnCode.isBlank()
                && hashSecret != null && !hashSecret.isBlank();
    }
}
