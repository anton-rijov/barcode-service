package com.x5.food.exception;

public class ApiResponseFormatException extends RuntimeException {
    public ApiResponseFormatException(String message) {
        super(message);
    }
}
