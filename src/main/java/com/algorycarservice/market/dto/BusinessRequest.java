package com.algorycarservice.market.dto;

import com.algorycarservice.market.model.ServiceCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** @param slotCapacity vehicles per slot; left out keeps the current value (1 for a new business) */
public record BusinessRequest(
        @NotBlank(message = "{validation.business-name.required}")
        @Size(max = 120, message = "{validation.business-name.size}")
        String name,

        @Size(max = 1000, message = "{validation.note.size}")
        String description,

        @NotBlank(message = "{validation.phone.required}")
        @Size(max = 20, message = "{validation.phone.size}")
        String phone,

        @NotBlank(message = "{validation.city.required}")
        @Size(max = 60, message = "{validation.city.size}")
        String city,

        @NotBlank(message = "{validation.district.required}")
        @Size(max = 60, message = "{validation.district.size}")
        String district,

        @NotBlank(message = "{validation.address.required}")
        @Size(max = 300, message = "{validation.address.size}")
        String address,

        @NotEmpty(message = "{validation.categories.required}")
        Set<ServiceCategory> categories,

        @Min(value = 1, message = "{validation.slot-capacity.range}")
        @Max(value = 10, message = "{validation.slot-capacity.range}")
        Integer slotCapacity
) {
}
