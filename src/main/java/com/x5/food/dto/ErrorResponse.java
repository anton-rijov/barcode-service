package com.x5.food.dto;

import java.time.LocalTime;

public record ErrorResponse(
        int status,
        LocalTime time,
        String message,
        String detail
) {

    public ErrorResponse(int status, String message, String detail) {
        this(status, LocalTime.now(), message, detail);

    }
}
