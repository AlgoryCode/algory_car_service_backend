package com.algorycarservice.leave.service;

import com.algorycarservice.common.config.AppProperties;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.leave.dto.LeaveBalanceResponse;
import com.algorycarservice.leave.dto.LeaveRequestDto;
import com.algorycarservice.leave.dto.LeaveResponse;
import com.algorycarservice.leave.model.LeaveDays;
import com.algorycarservice.leave.model.LeaveRequest;
import com.algorycarservice.leave.model.LeaveStatus;
import com.algorycarservice.leave.model.LeaveType;
import com.algorycarservice.leave.repository.LeaveRequestRepository;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.user.model.User;
import com.algorycarservice.user.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {

    /** Requests that still hold their days: they block overlapping requests and count against the allowance. */
    private static final Set<LeaveStatus> HOLDING = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository repository;
    private final StaffService staffService;
    private final AppProperties properties;

    public List<LeaveResponse> mine(Long userId) {
        return repository.findAllByUserIdOrderByStartDateDesc(userId).stream().map(LeaveResponse::from).toList();
    }

    public List<LeaveResponse> byStatus(Set<LeaveStatus> statuses) {
        Set<LeaveStatus> filter = statuses == null || statuses.isEmpty() ? EnumSet.allOf(LeaveStatus.class) : statuses;
        return repository.findAllByStatusInOrderByStartDateAsc(filter).stream().map(LeaveResponse::from).toList();
    }

    /** Everyone's approved and pending leaves touching the month, for the team calendar. */
    public List<LeaveResponse> calendar(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return repository.findOverlapping(HOLDING, yearMonth.atDay(1), yearMonth.atEndOfMonth()).stream()
                .map(LeaveResponse::from)
                .toList();
    }

    public List<LeaveResponse> onLeave(LocalDate day) {
        return repository.findOverlapping(EnumSet.of(LeaveStatus.APPROVED), day, day).stream()
                .map(LeaveResponse::from)
                .toList();
    }

    public long pendingCount() {
        return repository.countByStatus(LeaveStatus.PENDING);
    }

    public LeaveBalanceResponse balance(Long userId, int year) {
        User user = staffService.getUser(userId);
        int used = annualDays(userId, year, EnumSet.of(LeaveStatus.APPROVED));
        int pending = annualDays(userId, year, EnumSet.of(LeaveStatus.PENDING));
        return new LeaveBalanceResponse(year, user.getAnnualLeaveDays(), used, pending, user.getAnnualLeaveDays() - used);
    }

    @Transactional
    public LeaveResponse create(AuthUser actor, LeaveRequestDto request) {
        if (request.userId() != null && !request.userId().equals(actor.id()) && !actor.isAdmin()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        User user = staffService.getUser(request.userId() != null ? request.userId() : actor.id());
        int days = LeaveDays.count(request.startDate(), request.endDate());
        if (days == 0) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (repository.existsOverlapping(user.getId(), HOLDING, request.startDate(), request.endDate())) {
            throw new ApiException(ErrorCode.LEAVE_OVERLAPS);
        }
        if (request.type() == LeaveType.ANNUAL) {
            checkAllowance(user, request.startDate(), request.endDate());
        }

        LeaveRequest leave = LeaveRequest.builder()
                .user(user)
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .days(days)
                .reason(StringUtils.hasText(request.reason()) ? request.reason().trim() : null)
                .status(LeaveStatus.PENDING)
                .build();
        // An admin's own entry needs nobody else's approval.
        if (actor.isAdmin()) {
            review(leave, staffService.getUser(actor.id()), LeaveStatus.APPROVED, null);
        }
        return LeaveResponse.from(repository.save(leave));
    }

    @Transactional
    public LeaveResponse approve(Long reviewerId, Long id, String note) {
        return decide(reviewerId, id, LeaveStatus.APPROVED, note);
    }

    @Transactional
    public LeaveResponse reject(Long reviewerId, Long id, String note) {
        return decide(reviewerId, id, LeaveStatus.REJECTED, note);
    }

    /** Staff may withdraw their own pending requests; admins may remove any request. */
    @Transactional
    public void delete(AuthUser actor, Long id) {
        LeaveRequest leave = getLeave(id);
        if (!actor.isAdmin()) {
            if (!leave.getUser().getId().equals(actor.id())) {
                throw new ApiException(ErrorCode.ACCESS_DENIED);
            }
            if (leave.getStatus() != LeaveStatus.PENDING) {
                throw new ApiException(ErrorCode.LEAVE_ALREADY_REVIEWED);
            }
        }
        repository.delete(leave);
    }

    public LocalDate today() {
        return LocalDate.now(properties.timeZone());
    }

    private LeaveResponse decide(Long reviewerId, Long id, LeaveStatus decision, String note) {
        LeaveRequest leave = getLeave(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(ErrorCode.LEAVE_ALREADY_REVIEWED);
        }
        review(leave, staffService.getUser(reviewerId), decision, note);
        return LeaveResponse.from(leave);
    }

    private static void review(LeaveRequest leave, User reviewer, LeaveStatus decision, String note) {
        leave.setStatus(decision);
        leave.setReviewer(reviewer);
        leave.setReviewNote(StringUtils.hasText(note) ? note.trim() : null);
        leave.setReviewedAt(Instant.now());
    }

    /** Pending days count too, so two requests filed together cannot overdraw the allowance. */
    private void checkAllowance(User user, LocalDate start, LocalDate end) {
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            int requested = LeaveDays.countInYear(start, end, year);
            int held = annualDays(user.getId(), year, HOLDING);
            if (held + requested > user.getAnnualLeaveDays()) {
                throw new ApiException(ErrorCode.LEAVE_BALANCE_EXCEEDED);
            }
        }
    }

    private int annualDays(Long userId, int year, Set<LeaveStatus> statuses) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        return repository.findForBalance(userId, LeaveType.ANNUAL, statuses, from, to).stream()
                .mapToInt(leave -> LeaveDays.countInYear(leave.getStartDate(), leave.getEndDate(), year))
                .sum();
    }

    private LeaveRequest getLeave(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("leave"));
    }
}
