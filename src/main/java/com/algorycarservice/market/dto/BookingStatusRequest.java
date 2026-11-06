package com.algorycarservice.market.dto;

import com.algorycarservice.market.model.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param note optional reason, shown to the other side (e.g. why a request was rejected) */
public record BookingStatusRequest(
        @NotNull(message = "{validation.status.required}")
        BookingStatus status,

        @Size(max = 300, message = "{validation.note.size}")
        String note
) {
}
