package com.example.restaurantpro.exception;

public class TableInUseException extends RuntimeException {

    public TableInUseException(String message) {
        super(message);
    }
}
