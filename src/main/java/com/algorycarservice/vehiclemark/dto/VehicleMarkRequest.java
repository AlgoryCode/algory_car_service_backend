package com.algorycarservice.vehiclemark.dto;

import com.algorycarservice.vehiclemark.model.CarView;
import com.algorycarservice.vehiclemark.model.MarkType;
import com.algorycarservice.workorder.model.VehiclePart;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param x position across the drawing, 0 (left) to 1 (right); {@code y} likewise top to bottom
 * @param z depth across the 3D model's bounding box; required for {@code MODEL}, ignored otherwise
 */
public record VehicleMarkRequest(
        @NotNull(message = "{validation.mark-view.required}")
        CarView view,

        @NotNull(message = "{validation.mark-position.range}")
        @DecimalMin(value = "0.0", message = "{validation.mark-position.range}")
        @DecimalMax(value = "1.0", message = "{validation.mark-position.range}")
        Double x,

        @NotNull(message = "{validation.mark-position.range}")
        @DecimalMin(value = "0.0", message = "{validation.mark-position.range}")
        @DecimalMax(value = "1.0", message = "{validation.mark-position.range}")
        Double y,

        @DecimalMin(value = "0.0", message = "{validation.mark-position.range}")
        @DecimalMax(value = "1.0", message = "{validation.mark-position.range}")
        Double z,

        @NotNull(message = "{validation.mark-type.required}")
        MarkType type,

        VehiclePart part,

        @Size(max = 300, message = "{validation.note.size}")
        String note,

        Long workOrderId
) {

    @AssertTrue(message = "{validation.mark-position.range}")
    boolean isPositionComplete() {
        return view != CarView.MODEL || z != null;
    }
}
