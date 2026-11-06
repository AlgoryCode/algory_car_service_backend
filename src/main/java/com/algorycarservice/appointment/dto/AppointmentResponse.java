package com.algorycarservice.appointment.dto;

import com.algorycarservice.appointment.model.Appointment;
import com.algorycarservice.appointment.model.AppointmentStatus;
import com.algorycarservice.appointment.model.AppointmentType;
import com.algorycarservice.user.dto.UserRef;
import com.algorycarservice.vehicle.dto.VehicleRef;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** @param vehicle {@code null} while the car is not registered */
public record AppointmentResponse(
        Long id,
        VehicleRef vehicle,
        String plate,
        String customerName,
        String customerPhone,
        AppointmentType type,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String notes,
        String statusNote,
        UserRef createdBy,
        Long workOrderId,
        Instant createdAt
) {

    public static AppointmentResponse from(Appointment appointment, LocalTime endTime) {
        return new AppointmentResponse(appointment.getId(),
                appointment.getVehicle() == null ? null : VehicleRef.from(appointment.getVehicle()),
                appointment.getPlate(), appointment.getCustomerName(), appointment.getCustomerPhone(),
                appointment.getType(), appointment.getDate(), appointment.getStartTime(), endTime,
                appointment.getStatus(), appointment.getNotes(), appointment.getStatusNote(),
                UserRef.from(appointment.getCreatedBy()), appointment.getWorkOrderId(), appointment.getCreatedAt());
    }
}
