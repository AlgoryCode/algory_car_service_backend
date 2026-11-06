package com.algorycarservice.workorder.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PaymentRequest(@NotNull(message = "{validation.date.required}") LocalDate paidOn) {
}
