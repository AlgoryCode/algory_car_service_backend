package com.algorycarservice.workorder.dto;

import com.algorycarservice.workorder.model.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "{validation.status.required}") WorkOrderStatus status) {
}
