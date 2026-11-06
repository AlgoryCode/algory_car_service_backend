package com.algorycarservice.vehiclemark.dto;

import com.algorycarservice.vehiclemark.model.VehicleMarkPhoto;

import java.time.Instant;

/** The file is fetched with {@code GET /api/v1/vehicle-mark-photos/{id}}, so it never needs a public URL. */
public record MarkPhotoResponse(Long id, Instant createdAt) {

    public static MarkPhotoResponse from(VehicleMarkPhoto photo) {
        return new MarkPhotoResponse(photo.getId(), photo.getCreatedAt());
    }
}
