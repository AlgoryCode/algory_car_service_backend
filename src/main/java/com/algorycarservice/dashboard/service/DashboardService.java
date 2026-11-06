package com.algorycarservice.dashboard.service;

import com.algorycarservice.appointment.service.AppointmentService;
import com.algorycarservice.dashboard.dto.DashboardResponse;
import com.algorycarservice.finance.model.TransactionType;
import com.algorycarservice.finance.service.FinanceService;
import com.algorycarservice.leave.service.LeaveService;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.workorder.model.WorkOrderStatus;
import com.algorycarservice.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final WorkOrderService workOrderService;
    private final LeaveService leaveService;
    private final FinanceService financeService;
    private final AppointmentService appointmentService;

    public DashboardResponse dashboard(AuthUser user) {
        LocalDate today = leaveService.today();
        YearMonth month = YearMonth.from(today);
        boolean admin = user.isAdmin();
        return new DashboardResponse(
                workOrderService.count(WorkOrderStatus.OPEN),
                workOrderService.count(WorkOrderStatus.IN_PROGRESS),
                workOrderService.count(WorkOrderStatus.COMPLETED),
                leaveService.onLeave(today),
                appointmentService.forToday(),
                workOrderService.recent(),
                leaveService.balance(user.id(), today.getYear()),
                admin ? leaveService.pendingCount() : null,
                admin ? financeService.total(TransactionType.INCOME, month.atDay(1), month.atEndOfMonth()) : null,
                admin ? financeService.total(TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth()) : null);
    }
}
