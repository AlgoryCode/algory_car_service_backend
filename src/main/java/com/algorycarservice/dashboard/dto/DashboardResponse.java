package com.algorycarservice.dashboard.dto;

import com.algorycarservice.appointment.dto.AppointmentResponse;
import com.algorycarservice.leave.dto.LeaveBalanceResponse;
import com.algorycarservice.leave.dto.LeaveResponse;
import com.algorycarservice.workorder.dto.WorkOrderResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/** Admin-only figures ({@code pendingLeaves}, {@code monthIncome}, {@code monthExpense}) are left out for staff. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(
        long openCount,
        long inProgressCount,
        long readyCount,
        List<LeaveResponse> onLeaveToday,
        List<AppointmentResponse> todayAppointments,
        List<WorkOrderResponse> recentWorkOrders,
        LeaveBalanceResponse myLeaveBalance,
        Long pendingLeaves,
        BigDecimal monthIncome,
        BigDecimal monthExpense
) {
}
