package com.algorycarservice.vehicle.service;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.vehicle.dto.VehicleRequest;
import com.algorycarservice.vehicle.dto.VehicleResponse;
import com.algorycarservice.vehicle.model.Vehicle;
import com.algorycarservice.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final VehicleRepository vehicleRepository;

    public List<VehicleResponse> list(String query) {
        List<Vehicle> vehicles = StringUtils.hasText(query)
                ? vehicleRepository.search(query.trim().toLowerCase(TURKISH), normalizePlate(query))
                : vehicleRepository.findAllByOrderByCreatedAtDesc();
        return vehicles.stream().map(VehicleResponse::from).toList();
    }

    public VehicleResponse get(Long id) {
        return VehicleResponse.from(getVehicle(id));
    }

    /** Exact lookup for a typed or scanned plate; spacing and case do not matter. */
    public VehicleResponse getByPlate(String plate) {
        return vehicleRepository.findByPlate(normalizePlate(plate))
                .map(VehicleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("vehicle"));
    }

    public Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("vehicle"));
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        String plate = normalizePlate(request.plate());
        if (vehicleRepository.existsByPlate(plate)) {
            throw new ApiException(ErrorCode.PLATE_ALREADY_REGISTERED);
        }
        Vehicle vehicle = Vehicle.builder().build();
        apply(vehicle, request, plate);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = getVehicle(id);
        String plate = normalizePlate(request.plate());
        if (vehicleRepository.existsByPlateAndIdNot(plate, id)) {
            throw new ApiException(ErrorCode.PLATE_ALREADY_REGISTERED);
        }
        apply(vehicle, request, plate);
        return VehicleResponse.from(vehicle);
    }

    /** Work orders go with the vehicle (ON DELETE CASCADE); their finance entries stay in the books. */
    @Transactional
    public void delete(Long id) {
        vehicleRepository.delete(getVehicle(id));
    }

    public static String normalizePlate(String plate) {
        return plate.replaceAll("\\s+", "").toUpperCase(TURKISH);
    }

    private static void apply(Vehicle vehicle, VehicleRequest request, String plate) {
        vehicle.setPlate(plate);
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setModelYear(request.modelYear());
        vehicle.setColor(clean(request.color()));
        vehicle.setVin(StringUtils.hasText(request.vin()) ? request.vin().trim().toUpperCase(Locale.ROOT) : null);
        vehicle.setOwnerName(request.ownerName().trim());
        vehicle.setOwnerPhone(clean(request.ownerPhone()));
        vehicle.setNotes(clean(request.notes()));
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
