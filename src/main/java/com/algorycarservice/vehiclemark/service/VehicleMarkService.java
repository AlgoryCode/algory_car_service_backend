package com.algorycarservice.vehiclemark.service;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.common.storage.PhotoStorage;
import com.algorycarservice.common.storage.StorageProperties;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.user.service.StaffService;
import com.algorycarservice.vehicle.model.Vehicle;
import com.algorycarservice.vehicle.service.VehicleService;
import com.algorycarservice.vehiclemark.dto.MarkPhotoResponse;
import com.algorycarservice.vehiclemark.dto.VehicleMarkRequest;
import com.algorycarservice.vehiclemark.dto.VehicleMarkResponse;
import com.algorycarservice.vehiclemark.model.CarView;
import com.algorycarservice.vehiclemark.model.VehicleMark;
import com.algorycarservice.vehiclemark.model.VehicleMarkPhoto;
import com.algorycarservice.vehiclemark.repository.VehicleMarkPhotoRepository;
import com.algorycarservice.vehiclemark.repository.VehicleMarkRepository;
import com.algorycarservice.workorder.model.WorkOrder;
import com.algorycarservice.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleMarkService {

    /** Enough to show a mark from a few angles; more is usually the same photo again. */
    static final int MAX_PHOTOS_PER_MARK = 6;

    private final VehicleMarkRepository markRepository;
    private final VehicleMarkPhotoRepository photoRepository;
    private final WorkOrderRepository workOrderRepository;
    private final VehicleService vehicleService;
    private final StaffService staffService;
    private final PhotoStorage storage;
    private final StorageProperties storageProperties;

    public List<VehicleMarkResponse> list(Long vehicleId) {
        vehicleService.getVehicle(vehicleId);
        return markRepository.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(VehicleMarkResponse::from)
                .toList();
    }

    @Transactional
    public VehicleMarkResponse create(AuthUser user, Long vehicleId, VehicleMarkRequest request) {
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        VehicleMark mark = VehicleMark.builder()
                .vehicle(vehicle)
                .createdBy(staffService.getUser(user.id()))
                .build();
        apply(mark, request);
        return VehicleMarkResponse.from(markRepository.save(mark));
    }

    @Transactional
    public VehicleMarkResponse update(AuthUser user, Long id, VehicleMarkRequest request) {
        VehicleMark mark = getOwnedMark(user, id);
        apply(mark, request);
        return VehicleMarkResponse.from(mark);
    }

    @Transactional
    public void delete(AuthUser user, Long id) {
        VehicleMark mark = getOwnedMark(user, id);
        List<String> keys = mark.getPhotos().stream().map(VehicleMarkPhoto::getObjectKey).toList();
        markRepository.delete(mark);
        afterCommit(() -> removeStoredPhotos(keys));
    }

    /** Any member of staff may add photos to a mark, e.g. a second angle taken by the technician. */
    @Transactional
    public MarkPhotoResponse addPhoto(Long markId, byte[] bytes) {
        VehicleMark mark = getMark(markId);
        if (mark.getPhotos().size() >= MAX_PHOTOS_PER_MARK) {
            throw new ApiException(ErrorCode.PHOTO_LIMIT_REACHED);
        }
        if (bytes.length > storageProperties.maxFileSizeBytes()) {
            throw new ApiException(ErrorCode.PHOTO_TOO_LARGE);
        }
        ImageType type = ImageType.detect(bytes);
        if (type == null) {
            throw new ApiException(ErrorCode.PHOTO_INVALID);
        }
        String key = "vehicles/%d/marks/%d/%s.%s".formatted(mark.getVehicle().getId(), mark.getId(), UUID.randomUUID(), type.extension);
        storage.put(key, bytes, type.contentType);
        try {
            VehicleMarkPhoto photo = VehicleMarkPhoto.builder().mark(mark).objectKey(key).contentType(type.contentType).build();
            mark.getPhotos().add(photo);
            photoRepository.saveAndFlush(photo);
            return MarkPhotoResponse.from(photo);
        } catch (RuntimeException ex) {
            storage.deleteQuietly(key);
            throw ex;
        }
    }

    public StoredPhoto getPhoto(Long photoId) {
        VehicleMarkPhoto photo = getPhotoRow(photoId);
        return new StoredPhoto(storage.get(photo.getObjectKey()), photo.getContentType());
    }

    @Transactional
    public void deletePhoto(AuthUser user, Long photoId) {
        VehicleMarkPhoto photo = getPhotoRow(photoId);
        checkOwner(user, photo.getMark());
        photo.getMark().getPhotos().remove(photo);
        String key = photo.getObjectKey();
        afterCommit(() -> storage.deleteQuietly(key));
    }

    /** Taken before the vehicle row goes, since the cascade removes the photo rows that point at the files. */
    public List<String> photoKeysOfVehicle(Long vehicleId) {
        return markRepository.findPhotoKeysByVehicleId(vehicleId);
    }

    public void removeStoredPhotos(List<String> keys) {
        keys.forEach(storage::deleteQuietly);
    }

    private void apply(VehicleMark mark, VehicleMarkRequest request) {
        mark.setView(request.view());
        mark.setPosX(request.x());
        mark.setPosY(request.y());
        mark.setPosZ(request.view() == CarView.MODEL ? request.z() : null);
        mark.setType(request.type());
        mark.setPart(request.part());
        mark.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        mark.setWorkOrder(request.workOrderId() == null ? null : workOrderOf(mark.getVehicle(), request.workOrderId()));
    }

    private WorkOrder workOrderOf(Vehicle vehicle, Long workOrderId) {
        WorkOrder order = workOrderRepository.findById(workOrderId).orElseThrow(() -> new ResourceNotFoundException("work-order"));
        if (!Objects.equals(order.getVehicle().getId(), vehicle.getId())) {
            throw new ApiException(ErrorCode.WORK_ORDER_OTHER_VEHICLE);
        }
        return order;
    }

    private VehicleMark getMark(Long id) {
        return markRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("mark"));
    }

    private VehicleMarkPhoto getPhotoRow(Long id) {
        return photoRepository.findWithMarkById(id).orElseThrow(() -> new ResourceNotFoundException("photo"));
    }

    /** Staff can fix their own marks; anyone else's only the admin can change or remove. */
    private VehicleMark getOwnedMark(AuthUser user, Long id) {
        VehicleMark mark = getMark(id);
        checkOwner(user, mark);
        return mark;
    }

    private static void checkOwner(AuthUser user, VehicleMark mark) {
        boolean own = mark.getCreatedBy() != null && Objects.equals(mark.getCreatedBy().getId(), user.id());
        if (!own && !user.isAdmin()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
    }

    /** Files are removed only once the rows are really gone, so a rolled-back delete keeps its photos. */
    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public record StoredPhoto(byte[] bytes, String contentType) {
    }

    /** Checked from the file's first bytes rather than the name or header the client sent. */
    private enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageType detect(byte[] b) {
            if (b.length < 12) {
                return null;
            }
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
                return JPEG;
            }
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return PNG;
            }
            if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
