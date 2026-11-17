package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.Equipment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    Optional<Equipment> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.equipmentId = :id")
    Optional<Equipment> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM Equipment e " +
           "JOIN MaintenanceWorkOrder w ON w.equipment = e " +
           "JOIN WorkOrderAssignment a ON a.workOrder = w " +
           "WHERE a.technician.userId = :technicianId")
    Page<Equipment> findAssignedToTechnician(@Param("technicianId") Long technicianId, Pageable pageable);

    long countByStatus(com.fpt.ems.be.model.enums.EquipmentStatus status);
}
