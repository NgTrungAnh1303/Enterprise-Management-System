package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
    java.util.List<AuditLog> findByActionOrderByOccurredAtDesc(String action);
}


