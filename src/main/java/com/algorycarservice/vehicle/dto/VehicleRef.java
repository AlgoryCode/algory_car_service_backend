package com.algorycarservice.vehicle.dto;

import com.algorycarservice.vehicle.model.Vehicle;

/** Vehicle summary embedded in work orders. */
public record VehicleRef(Long id, String plate, String brand, String model, String ownerName, String ownerPhone) {

    public static VehicleRef from(Vehicle vehicle) {
        return new VehicleRef(vehicle.getId(), vehicle.getPlate(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getOwnerName(), vehicle.getOwnerPhone());
    }
}
