package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByRoomAndArea(String room, String area);
    boolean existsByRoomAndArea(String room, String area);
    boolean existsByRoomAndAreaAndLocationIdNot(String room, String area, Long locationId);
}
