package com.algorycarservice.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Bookable times of one day.
 *
 * @param closed   the workshop does not work that day (Sunday); {@code slots} is then empty
 * @param capacity how many vehicles can be booked into the same slot
 */
public record SlotsResponse(LocalDate date, boolean closed, int capacity, List<Slot> slots) {

    /** @param available free and not already in the past */
    public record Slot(LocalTime time, int booked, boolean available) {
    }
}
