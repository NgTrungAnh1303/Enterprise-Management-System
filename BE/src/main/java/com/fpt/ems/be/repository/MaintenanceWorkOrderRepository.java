package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceWorkOrderRepository extends JpaRepository<MaintenanceWorkOrder, Long>, JpaSpecificationExecutor<MaintenanceWorkOrder> {

    Optional<MaintenanceWorkOrder> findByWoCode(String woCode);

    boolean existsByWoCode(String woCode);

    boolean existsByEquipment_EquipmentIdAndPeriodAndTypeAndStatusNot(
            Long equipmentId, String period, WorkOrderType type, WorkOrderStatus status);

    boolean existsByEquipment_EquipmentIdAndTypeAndStatusNot(
            Long equipmentId, WorkOrderType type, WorkOrderStatus status);

    long countByEquipment_EquipmentIdAndTypeAndStatusNot(
            Long equipmentId, WorkOrderType type, WorkOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM MaintenanceWorkOrder w WHERE w.woId = :id")
    Optional<MaintenanceWorkOrder> findByIdWithLock(@Param("id") Long id);

    java.util.List<MaintenanceWorkOrder> findByIncident_IncidentId(Long incidentId);

    Optional<MaintenanceWorkOrder> findFirstByIncident_IncidentIdOrderByCreatedAtDesc(Long incidentId);

    @Query("SELECT w.status, COUNT(w) FROM MaintenanceWorkOrder w WHERE w.createdAt >= :startOfMonth AND w.createdAt < :startOfNextMonth GROUP BY w.status")
    java.util.List<Object[]> countWorkOrdersByStatusInPeriod(
            @Param("startOfMonth") java.time.Instant startOfMonth,
            @Param("startOfNextMonth") java.time.Instant startOfNextMonth
    );

    @Query("SELECT COUNT(w) FROM MaintenanceWorkOrder w WHERE w.type = 'PERIODIC' AND w.status <> 'CLOSED' AND w.plan.nextDueDate < :today")
    long countOverduePeriodicWorkOrders(@Param("today") java.time.LocalDate today);

    @Query("SELECT COUNT(w) FROM MaintenanceWorkOrder w WHERE w.type = 'PERIODIC' AND w.status <> 'CLOSED' AND w.plan.nextDueDate >= :today AND w.plan.nextDueDate <= :dueSoonDate")
    long countDueSoonPeriodicWorkOrders(
            @Param("today") java.time.LocalDate today,
            @Param("dueSoonDate") java.time.LocalDate dueSoonDate
    );
}
