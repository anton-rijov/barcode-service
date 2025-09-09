package com.x5.food.dto;

public record ErrorResponse(
        int status,
        String message) {
}
