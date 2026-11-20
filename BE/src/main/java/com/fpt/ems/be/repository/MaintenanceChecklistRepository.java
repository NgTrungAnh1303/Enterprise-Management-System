package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.MaintenanceChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceChecklistRepository extends JpaRepository<MaintenanceChecklist, Long> {
    List<MaintenanceChecklist> findByEquipmentType_TypeId(Long typeId);
}

