package com.algorycarservice.vehicle.repository;

import com.algorycarservice.vehicle.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlate(String plate);

    Optional<Vehicle> findByPlate(String plate);

    boolean existsByPlateAndIdNot(String plate, Long id);

    List<Vehicle> findAllByOrderByCreatedAtDesc();

    /** {@code plateQuery} is the search text normalised like plates, so "34 abc" finds 34ABC123. */
    @Query("""
            select v from Vehicle v
            where v.plate like concat('%', :plateQuery, '%')
               or lower(v.ownerName) like concat('%', :query, '%')
               or lower(v.brand) like concat('%', :query, '%')
               or lower(v.model) like concat('%', :query, '%')
            order by v.createdAt desc
            """)
    List<Vehicle> search(@Param("query") String query, @Param("plateQuery") String plateQuery);
}
