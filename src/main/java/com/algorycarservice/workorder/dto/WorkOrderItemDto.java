package com.algorycarservice.workorder.dto;

import com.algorycarservice.workorder.model.ItemKind;
import com.algorycarservice.workorder.model.PartOperation;
import com.algorycarservice.workorder.model.VehiclePart;
import com.algorycarservice.workorder.model.WorkOrderItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param part      where on the vehicle the line was done; {@code null} for general work
 * @param operation what was done to {@code part}; dropped when no part is given
 * @param total     read-only in responses; ignored on requests
 */
public record WorkOrderItemDto(
        @NotNull(message = "{validation.item-kind.required}")
        ItemKind kind,

        @NotBlank(message = "{validation.item-description.required}")
        @Size(max = 150, message = "{validation.item-description.size}")
        String description,

        @NotNull(message = "{validation.quantity.required}")
        @DecimalMin(value = "0.01", message = "{validation.quantity.positive}")
        @Digits(integer = 6, fraction = 2, message = "{validation.amount.digits}")
        BigDecimal quantity,

        @NotNull(message = "{validation.amount.required}")
        @DecimalMin(value = "0.0", message = "{validation.amount.negative}")
        @Digits(integer = 10, fraction = 2, message = "{validation.amount.digits}")
        BigDecimal unitPrice,

        VehiclePart part,

        PartOperation operation,

        BigDecimal total
) {

    public static WorkOrderItemDto from(WorkOrderItem item) {
        return new WorkOrderItemDto(item.getKind(), item.getDescription(), item.getQuantity(), item.getUnitPrice(),
                item.getPart(), item.getOperation(), item.total());
    }

    public WorkOrderItem toItem() {
        return new WorkOrderItem(kind, description.trim(), quantity, unitPrice, part, part == null ? null : operation);
    }
}
