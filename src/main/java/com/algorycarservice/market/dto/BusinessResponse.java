package com.algorycarservice.market.dto;

import com.algorycarservice.market.model.Business;
import com.algorycarservice.market.model.ServiceCategory;

import java.util.List;
import java.util.UUID;

public record BusinessResponse(
        Long id,
        UUID uuid,
        String name,
        String description,
        String phone,
        String city,
        String district,
        String address,
        List<ServiceCategory> categories,
        int slotCapacity,
        boolean active
) {

    public static BusinessResponse from(Business business) {
        return new BusinessResponse(business.getId(), business.getUuid(), business.getName(), business.getDescription(),
                business.getPhone(), business.getCity(), business.getDistrict(), business.getAddress(),
                business.getCategories().stream().sorted().toList(), business.getSlotCapacity(), business.isActive());
    }
}
