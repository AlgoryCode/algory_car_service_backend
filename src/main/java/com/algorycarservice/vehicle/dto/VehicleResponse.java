package com.algorycarservice.vehicle.dto;

import com.algorycarservice.vehicle.model.Vehicle;

import java.time.Instant;

public record VehicleResponse(
        Long id,
        String plate,
        String brand,
        String model,
        Integer modelYear,
        String color,
        String vin,
        String ownerName,
        String ownerPhone,
        String notes,
        Instant createdAt
) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getPlate(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getModelYear(), vehicle.getColor(), vehicle.getVin(), vehicle.getOwnerName(),
                vehicle.getOwnerPhone(), vehicle.getNotes(), vehicle.getCreatedAt());
    }
}
