package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.MaintenancePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {
    Optional<MaintenancePlan> findByEquipment_EquipmentId(Long equipmentId);
}

