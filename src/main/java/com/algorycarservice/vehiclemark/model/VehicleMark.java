package com.algorycarservice.vehiclemark.model;

import com.algorycarservice.user.model.User;
import com.algorycarservice.vehicle.model.Vehicle;
import com.algorycarservice.workorder.model.VehiclePart;
import com.algorycarservice.workorder.model.WorkOrder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Something noticed on the vehicle, pinned where it is on one of the drawings, like the damage sketch on a rental
 * hand-over form. {@code posX}/{@code posY} (and {@code posZ} on the 3D model) run from 0 to 1 across the drawing or the model's
 * bounding box, so they survive any screen size or model scale.
 */
@Entity
@Table(name = "vehicle_marks")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VehicleMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, updatable = false)
    private Vehicle vehicle;

    /** The visit it was found or dealt with on, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_view", nullable = false)
    private CarView view;

    @Column(name = "pos_x", nullable = false)
    private double posX;

    @Column(name = "pos_y", nullable = false)
    private double posY;

    /** Depth on the 3D model; {@code null} on the flat drawings. */
    @Column(name = "pos_z")
    private Double posZ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarkType type;

    /** The panel under the pin, worked out by the app from where it was placed. */
    @Enumerated(EnumType.STRING)
    private VehiclePart part;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "mark", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<VehicleMarkPhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
