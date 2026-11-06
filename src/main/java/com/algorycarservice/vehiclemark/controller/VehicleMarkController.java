package com.algorycarservice.vehiclemark.controller;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.vehiclemark.dto.MarkPhotoResponse;
import com.algorycarservice.vehiclemark.dto.VehicleMarkRequest;
import com.algorycarservice.vehiclemark.dto.VehicleMarkResponse;
import com.algorycarservice.vehiclemark.service.VehicleMarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/** Damage and notes pinned on the vehicle drawings, with their photos. */
@RestController
@RequiredArgsConstructor
public class VehicleMarkController {

    private final VehicleMarkService markService;

    @GetMapping("/api/v1/vehicles/{vehicleId}/marks")
    public List<VehicleMarkResponse> list(@PathVariable Long vehicleId) {
        return markService.list(vehicleId);
    }

    @PostMapping("/api/v1/vehicles/{vehicleId}/marks")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleMarkResponse create(@AuthenticationPrincipal AuthUser user, @PathVariable Long vehicleId,
                                      @Valid @RequestBody VehicleMarkRequest request) {
        return markService.create(user, vehicleId, request);
    }

    @PutMapping("/api/v1/vehicle-marks/{id}")
    public VehicleMarkResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                      @Valid @RequestBody VehicleMarkRequest request) {
        return markService.update(user, id, request);
    }

    @DeleteMapping("/api/v1/vehicle-marks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        markService.delete(user, id);
    }

    @PostMapping(value = "/api/v1/vehicle-marks/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MarkPhotoResponse addPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.PHOTO_INVALID);
        }
        try {
            return markService.addPhoto(id, file.getBytes());
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.PHOTO_INVALID);
        }
    }

    /** Served through the API so photos of customers' cars stay behind the login; the key never changes. */
    @GetMapping("/api/v1/vehicle-mark-photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable Long photoId) {
        VehicleMarkService.StoredPhoto photo = markService.getPhoto(photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate().immutable())
                .body(photo.bytes());
    }

    @DeleteMapping("/api/v1/vehicle-mark-photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@AuthenticationPrincipal AuthUser user, @PathVariable Long photoId) {
        markService.deletePhoto(user, photoId);
    }
}
