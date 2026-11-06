package com.algorycarservice.vehicle.controller;

import com.algorycarservice.vehicle.dto.VehicleRequest;
import com.algorycarservice.vehicle.dto.VehicleResponse;
import com.algorycarservice.vehicle.service.VehicleService;
import com.algorycarservice.vehiclemark.service.VehicleMarkService;
import com.algorycarservice.workorder.dto.WorkOrderResponse;
import com.algorycarservice.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final WorkOrderService workOrderService;
    private final VehicleMarkService markService;

    @GetMapping
    public List<VehicleResponse> list(@RequestParam(required = false) String q) {
        return vehicleService.list(q);
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable Long id) {
        return vehicleService.get(id);
    }

    /** Opens a vehicle straight from its plate, typed or read from the QR sticker. */
    @GetMapping("/by-plate/{plate}")
    public VehicleResponse getByPlate(@PathVariable String plate) {
        return vehicleService.getByPlate(plate);
    }

    /** Service history of the vehicle, newest first. */
    @GetMapping("/{id}/work-orders")
    public List<WorkOrderResponse> workOrders(@PathVariable Long id) {
        return workOrderService.listForVehicle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        List<String> photoKeys = markService.photoKeysOfVehicle(id);
        vehicleService.delete(id);
        markService.removeStoredPhotos(photoKeys);
    }
}
