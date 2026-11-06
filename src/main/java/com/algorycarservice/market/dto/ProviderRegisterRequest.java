package com.algorycarservice.market.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A business owner signs up together with the business they list. */
public record ProviderRegisterRequest(
        @NotBlank(message = "{validation.full-name.required}")
        @Size(max = 100, message = "{validation.full-name.size}")
        String fullName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 160, message = "{validation.email.size}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 72, message = "{validation.password.size}")
        String password,

        @NotNull(message = "{validation.business.required}")
        @Valid
        BusinessRequest business
) {
}
