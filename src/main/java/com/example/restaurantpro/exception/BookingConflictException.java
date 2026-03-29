package com.example.restaurantpro.exception;

import com.example.restaurantpro.dto.BookingSuggestionDto;

public class BookingConflictException extends RuntimeException {

    private final BookingSuggestionDto suggestions;

    public BookingConflictException(String message, BookingSuggestionDto suggestions) {
        super(message);
        this.suggestions = suggestions;
    }

    public BookingSuggestionDto getSuggestions() {
        return suggestions;
    }
}
