package com.algorycarservice.vehiclemark.dto;

import com.algorycarservice.user.dto.UserRef;
import com.algorycarservice.vehiclemark.model.CarView;
import com.algorycarservice.vehiclemark.model.MarkType;
import com.algorycarservice.vehiclemark.model.VehicleMark;
import com.algorycarservice.workorder.model.VehiclePart;

import java.time.Instant;
import java.util.List;

public record VehicleMarkResponse(
        Long id,
        Long vehicleId,
        CarView view,
        double x,
        double y,
        Double z,
        MarkType type,
        VehiclePart part,
        String note,
        Long workOrderId,
        UserRef createdBy,
        Instant createdAt,
        List<MarkPhotoResponse> photos
) {

    public static VehicleMarkResponse from(VehicleMark mark) {
        return new VehicleMarkResponse(mark.getId(), mark.getVehicle().getId(), mark.getView(), mark.getPosX(),
                mark.getPosY(), mark.getPosZ(), mark.getType(), mark.getPart(), mark.getNote(),
                mark.getWorkOrder() == null ? null : mark.getWorkOrder().getId(), UserRef.from(mark.getCreatedBy()),
                mark.getCreatedAt(), mark.getPhotos().stream().map(MarkPhotoResponse::from).toList());
    }
}
