package com.algorycarservice.workorder.repository;

import com.algorycarservice.workorder.model.WorkOrder;
import com.algorycarservice.workorder.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @EntityGraph(attributePaths = {"vehicle", "technician"})
    List<WorkOrder> findAllByStatusInOrderByOpenedOnDescIdDesc(Collection<WorkOrderStatus> statuses);

    @EntityGraph(attributePaths = {"vehicle", "technician"})
    List<WorkOrder> findAllByVehicleIdOrderByOpenedOnDescIdDesc(Long vehicleId);

    @EntityGraph(attributePaths = {"vehicle", "technician"})
    List<WorkOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(WorkOrderStatus status);
}
