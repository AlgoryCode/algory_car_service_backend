package com.algorycarservice.vehiclemark.repository;

import com.algorycarservice.vehiclemark.model.VehicleMark;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleMarkRepository extends JpaRepository<VehicleMark, Long> {

    @EntityGraph(attributePaths = {"createdBy", "workOrder"})
    List<VehicleMark> findAllByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    @Query("select p.objectKey from VehicleMarkPhoto p where p.mark.vehicle.id = :vehicleId")
    List<String> findPhotoKeysByVehicleId(@Param("vehicleId") Long vehicleId);
}
