package com.algorycarservice.leave.repository;

import com.algorycarservice.leave.model.LeaveRequest;
import com.algorycarservice.leave.model.LeaveStatus;
import com.algorycarservice.leave.model.LeaveType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<LeaveRequest> findAllByUserIdOrderByStartDateDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<LeaveRequest> findAllByStatusInOrderByStartDateAsc(Collection<LeaveStatus> statuses);

    long countByStatus(LeaveStatus status);

    /** Requests touching {@code from..to}; used by the calendar and the overlap check. */
    @EntityGraph(attributePaths = {"user", "reviewer"})
    @Query("""
            select l from LeaveRequest l
            where l.status in :statuses and l.startDate <= :to and l.endDate >= :from
            order by l.startDate, l.user.fullName
            """)
    List<LeaveRequest> findOverlapping(@Param("statuses") Collection<LeaveStatus> statuses,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query("""
            select count(l) > 0 from LeaveRequest l
            where l.user.id = :userId and l.status in :statuses and l.startDate <= :to and l.endDate >= :from
            """)
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("statuses") Collection<LeaveStatus> statuses,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    @Query("""
            select l from LeaveRequest l
            where l.user.id = :userId and l.type = :type and l.status in :statuses
              and l.startDate <= :to and l.endDate >= :from
            """)
    List<LeaveRequest> findForBalance(@Param("userId") Long userId,
                                      @Param("type") LeaveType type,
                                      @Param("statuses") Collection<LeaveStatus> statuses,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
