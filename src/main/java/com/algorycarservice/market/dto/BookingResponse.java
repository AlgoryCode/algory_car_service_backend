package com.algorycarservice.market.dto;

import com.algorycarservice.market.model.Booking;
import com.algorycarservice.market.model.BookingStatus;
import com.algorycarservice.market.model.Business;
import com.algorycarservice.market.model.ServiceCategory;
import com.algorycarservice.user.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** The same view for both sides: the customer needs the business's address, the business the customer's phone. */
public record BookingResponse(
        Long id,
        BusinessRef business,
        CustomerRef customer,
        ServiceCategory category,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String plate,
        String vehicle,
        String note,
        BookingStatus status,
        String statusNote,
        Instant createdAt
) {

    public record BusinessRef(Long id, String name, String phone, String city, String district, String address) {

        static BusinessRef from(Business business) {
            return new BusinessRef(business.getId(), business.getName(), business.getPhone(), business.getCity(),
                    business.getDistrict(), business.getAddress());
        }
    }

    public record CustomerRef(Long id, String fullName, String phone) {

        static CustomerRef from(User user) {
            return new CustomerRef(user.getId(), user.getFullName(), user.getPhone());
        }
    }

    public static BookingResponse from(Booking booking, LocalTime endTime) {
        return new BookingResponse(booking.getId(), BusinessRef.from(booking.getBusiness()),
                CustomerRef.from(booking.getCustomer()), booking.getCategory(), booking.getDate(),
                booking.getStartTime(), endTime, booking.getPlate(), booking.getVehicle(), booking.getNote(),
                booking.getStatus(), booking.getStatusNote(), booking.getCreatedAt());
    }
}
