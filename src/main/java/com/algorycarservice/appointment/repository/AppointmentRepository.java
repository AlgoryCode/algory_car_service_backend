package com.algorycarservice.appointment.repository;

import com.algorycarservice.appointment.model.Appointment;
import com.algorycarservice.appointment.model.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"vehicle", "createdBy"})
    @Query("""
            select a from Appointment a
            where a.date between :from and :to and a.status in :statuses
            order by a.date, a.startTime, a.id
            """)
    List<Appointment> findInRange(@Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("statuses") Collection<AppointmentStatus> statuses);

    @EntityGraph(attributePaths = {"vehicle", "createdBy"})
    List<Appointment> findAllByVehicleIdOrderByDateDescStartTimeDesc(Long vehicleId);

    /** Active bookings of the day; {@code excludeId} leaves out the appointment being edited (pass -1 for none). */
    @Query("""
            select a from Appointment a
            where a.date = :date and a.status in :statuses and a.id <> :excludeId
            """)
    List<Appointment> findOnDay(@Param("date") LocalDate date,
                                @Param("statuses") Collection<AppointmentStatus> statuses,
                                @Param("excludeId") Long excludeId);
}
