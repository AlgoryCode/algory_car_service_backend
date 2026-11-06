package com.algorycarservice.leave.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** The workshop works Monday to Saturday, so a leave counts every day of the range except Sundays. */
public final class LeaveDays {

    private LeaveDays() {
    }

    public static int count(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            return 0;
        }
        return (int) start.datesUntil(end.plusDays(1))
                .filter(day -> day.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }

    /** Days of {@code start..end} that fall into {@code year}. */
    public static int countInYear(LocalDate start, LocalDate end, int year) {
        LocalDate from = start.getYear() < year ? LocalDate.of(year, 1, 1) : start;
        LocalDate to = end.getYear() > year ? LocalDate.of(year, 12, 31) : end;
        return count(from, to);
    }
}
