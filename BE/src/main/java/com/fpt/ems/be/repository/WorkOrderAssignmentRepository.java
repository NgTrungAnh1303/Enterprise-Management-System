package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.WorkOrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderAssignmentRepository extends JpaRepository<WorkOrderAssignment, Long> {
    Optional<WorkOrderAssignment> findByWorkOrder_WoIdAndIsCurrentTrue(Long woId);
    List<WorkOrderAssignment> findByWorkOrder_WoId(Long woId);
    boolean existsByWorkOrder_WoIdAndTechnician_UserId(Long woId, Long technicianId);
    boolean existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(Long equipmentId, Long technicianId);
    boolean existsByWorkOrder_Incident_IncidentIdAndTechnician_UserId(Long incidentId, Long technicianId);
}
