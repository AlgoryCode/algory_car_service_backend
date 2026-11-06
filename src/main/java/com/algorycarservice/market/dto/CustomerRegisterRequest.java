package com.algorycarservice.market.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
        @NotBlank(message = "{validation.full-name.required}")
        @Size(max = 100, message = "{validation.full-name.size}")
        String fullName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 160, message = "{validation.email.size}")
        String email,

        @NotBlank(message = "{validation.phone.required}")
        @Size(max = 20, message = "{validation.phone.size}")
        String phone,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 72, message = "{validation.password.size}")
        String password
) {
}
