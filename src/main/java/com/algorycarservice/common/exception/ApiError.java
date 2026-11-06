package com.algorycarservice.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> errors
) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message, String path) {
        return of(code, message, path, List.of());
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldViolation> errors) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(code.getStatus().value())
                .code(code.name())
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }
}
