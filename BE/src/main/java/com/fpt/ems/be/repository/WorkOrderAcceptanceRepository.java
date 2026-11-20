package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.WorkOrderAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkOrderAcceptanceRepository extends JpaRepository<WorkOrderAcceptance, Long> {
    List<WorkOrderAcceptance> findByWorkOrder_WoIdOrderByAcceptedAtDesc(Long woId);
}

