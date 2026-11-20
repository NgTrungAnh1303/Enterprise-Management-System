package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.MaintenanceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    Page<MaintenanceHistory> findByEquipment_EquipmentIdOrderByPerformedAtDesc(Long equipmentId, Pageable pageable);
}

