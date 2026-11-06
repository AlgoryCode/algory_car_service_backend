package com.algorycarservice.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

/** @param timeZone the workshop's local zone; "today" for dashboards and leave calendars is taken from it */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Jwt jwt,
        @NotNull ZoneId timeZone,
        @Valid @NotNull Appointments appointments
) {

    /**
     * Booking calendar of the workshop.
     *
     * @param slotCapacity vehicles that can be booked into the same slot (lifts / inspection lanes)
     */
    public record Appointments(
            @NotNull @DateTimeFormat(pattern = "HH:mm") LocalTime openingTime,
            @NotNull @DateTimeFormat(pattern = "HH:mm") LocalTime closingTime,
            @NotNull Duration slotLength,
            @Positive int slotCapacity
    ) {
    }

    public record Jwt(
            @NotBlank String secret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl
    ) {
    }
}
