package com.example.restaurantpro.dto;

import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.PaymentTransaction;

public class VnPayCallbackResult {

    private final boolean validSignature;
    private final boolean success;
    private final String message;
    private final Booking booking;
    private final PaymentTransaction paymentTransaction;

    public VnPayCallbackResult(boolean validSignature,
                               boolean success,
                               String message,
                               Booking booking,
                               PaymentTransaction paymentTransaction) {
        this.validSignature = validSignature;
        this.success = success;
        this.message = message;
        this.booking = booking;
        this.paymentTransaction = paymentTransaction;
    }

    public boolean isValidSignature() {
        return validSignature;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Booking getBooking() {
        return booking;
    }

    public PaymentTransaction getPaymentTransaction() {
        return paymentTransaction;
    }
}
