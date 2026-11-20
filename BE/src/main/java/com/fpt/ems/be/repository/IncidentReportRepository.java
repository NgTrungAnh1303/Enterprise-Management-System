package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.IncidentReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long>, JpaSpecificationExecutor<IncidentReport> {

    Optional<IncidentReport> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IncidentReport i WHERE i.incidentId = :id")
    Optional<IncidentReport> findByIdWithLock(@Param("id") Long id);

    long countByStatusNot(com.fpt.ems.be.model.enums.IncidentStatus status);
}
