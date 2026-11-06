package com.algorycarservice.common.schedule;

import com.algorycarservice.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bookable times shared by the workshop's own appointments and OtoViser bookings: opening hours split into
 * slots of {@code app.appointments.slot-length}, Monday to Saturday, in the app's time zone.
 */
@Component
@RequiredArgsConstructor
public class SlotCalendar {

    private final AppProperties properties;

    /** Start times from opening until the last slot that still ends by closing time. */
    public List<LocalTime> slotTimes() {
        AppProperties.Appointments config = properties.appointments();
        int step = (int) config.slotLength().toMinutes();
        int close = config.closingTime().toSecondOfDay() / 60;
        List<LocalTime> times = new ArrayList<>();
        for (int minute = config.openingTime().toSecondOfDay() / 60; minute + step <= close; minute += step) {
            times.add(LocalTime.of(minute / 60, minute % 60));
        }
        return times;
    }

    public boolean isSlot(LocalTime time) {
        return slotTimes().contains(time);
    }

    /** Same week as the leave rules: work runs Monday to Saturday. */
    public boolean isClosed(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public boolean isUpcoming(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).isAfter(LocalDateTime.now(properties.timeZone()));
    }

    public LocalDate today() {
        return LocalDate.now(properties.timeZone());
    }

    public Duration slotLength() {
        return properties.appointments().slotLength();
    }
}
