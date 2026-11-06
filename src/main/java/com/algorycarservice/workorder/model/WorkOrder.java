package com.algorycarservice.workorder.model;

import com.algorycarservice.user.model.User;
import com.algorycarservice.vehicle.model.Vehicle;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Everything done to a vehicle during one visit; the vehicle's service history is the list of these. */
@Entity
@Table(name = "work_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, updatable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    /** What the customer reported, e.g. "Fren balatası sesi". */
    @Column(nullable = false)
    private String complaint;

    /** What the workshop found and did. */
    private String notes;

    private Integer mileageKm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @Column(nullable = false)
    private LocalDate openedOn;

    private LocalDate completedOn;

    /** Set while the payment is booked as income in the finance ledger. */
    private LocalDate paidOn;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "work_order_items", joinColumns = @JoinColumn(name = "work_order_id"))
    @OrderColumn(name = "line_no")
    private List<WorkOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public BigDecimal total() {
        return items.stream().map(WorkOrderItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void replaceItems(List<WorkOrderItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }
}
