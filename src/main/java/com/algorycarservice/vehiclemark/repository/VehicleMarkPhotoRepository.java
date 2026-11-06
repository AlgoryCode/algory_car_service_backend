package com.algorycarservice.vehiclemark.repository;

import com.algorycarservice.vehiclemark.model.VehicleMarkPhoto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleMarkPhotoRepository extends JpaRepository<VehicleMarkPhoto, Long> {

    @EntityGraph(attributePaths = {"mark", "mark.createdBy"})
    Optional<VehicleMarkPhoto> findWithMarkById(Long id);
}
