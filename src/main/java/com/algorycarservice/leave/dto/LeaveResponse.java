package com.algorycarservice.leave.dto;

import com.algorycarservice.leave.model.LeaveRequest;
import com.algorycarservice.leave.model.LeaveStatus;
import com.algorycarservice.leave.model.LeaveType;
import com.algorycarservice.user.dto.UserRef;

import java.time.Instant;
import java.time.LocalDate;

public record LeaveResponse(
        Long id,
        UserRef user,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        int days,
        String reason,
        LeaveStatus status,
        UserRef reviewer,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt
) {

    public static LeaveResponse from(LeaveRequest leave) {
        return new LeaveResponse(leave.getId(), UserRef.from(leave.getUser()), leave.getType(), leave.getStartDate(),
                leave.getEndDate(), leave.getDays(), leave.getReason(), leave.getStatus(),
                UserRef.from(leave.getReviewer()), leave.getReviewNote(), leave.getReviewedAt(), leave.getCreatedAt());
    }
}
