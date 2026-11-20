package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.WorkOrderAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkOrderAdjustmentRepository extends JpaRepository<WorkOrderAdjustment, Long> {
    List<WorkOrderAdjustment> findByWorkOrder_WoIdOrderByCreatedAtDesc(Long woId);
}

