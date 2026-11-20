package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.InspectionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionResultRepository extends JpaRepository<InspectionResult, Long> {
    List<InspectionResult> findByWorkOrder_WoId(Long woId);
    Optional<InspectionResult> findByWorkOrder_WoIdAndChecklist_ChecklistId(Long woId, Long checklistId);
    boolean existsByWorkOrder_WoIdAndEnteredBy_UserId(Long woId, Long userId);
}
