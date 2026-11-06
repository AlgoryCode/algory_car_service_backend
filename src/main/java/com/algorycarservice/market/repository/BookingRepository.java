package com.algorycarservice.market.repository;

import com.algorycarservice.market.model.Booking;
import com.algorycarservice.market.model.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByBusinessIdAndDateAndStatusIn(Long businessId, LocalDate date, Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"business", "customer"})
    List<Booking> findAllByCustomerIdOrderByDateDescStartTimeDesc(Long customerId);

    @EntityGraph(attributePaths = {"business", "customer"})
    List<Booking> findAllByBusinessIdAndStatusInOrderByDateAscStartTimeAsc(Long businessId, Collection<BookingStatus> statuses);
}
