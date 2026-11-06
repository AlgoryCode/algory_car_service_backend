package com.algorycarservice.vehicle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank(message = "{validation.plate.required}")
        @Size(max = 16, message = "{validation.plate.size}")
        String plate,

        @NotBlank(message = "{validation.brand.required}")
        @Size(max = 60, message = "{validation.brand.size}")
        String brand,

        @NotBlank(message = "{validation.model.required}")
        @Size(max = 60, message = "{validation.model.size}")
        String model,

        @Min(value = 1950, message = "{validation.model-year.range}")
        @Max(value = 2100, message = "{validation.model-year.range}")
        Integer modelYear,

        @Size(max = 30, message = "{validation.color.size}")
        String color,

        @Size(max = 17, message = "{validation.vin.size}")
        String vin,

        @NotBlank(message = "{validation.owner-name.required}")
        @Size(max = 100, message = "{validation.owner-name.size}")
        String ownerName,

        @Size(max = 20, message = "{validation.phone.size}")
        String ownerPhone,

        @Size(max = 500, message = "{validation.note.size}")
        String notes
) {
}
