package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.audit.AuditLogResponse;
import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.repository.AuditLogRepository;
import com.fpt.ems.be.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Value("${app.audit.manager-can-view:false}")
    private boolean managerCanView;

    @Transactional
    public PageResponse<AuditLogResponse> getAuditLogs(
            UserPrincipal principal,
            Long actorId,
            String action,
            AuditOutcome outcome,
            Instant fromDate,
            Instant toDate,
            int page,
            int size
    ) {
        String role = principal.getRole();
        if (RoleName.ADMIN.name().equals(role)) {
            // ADMIN is always allowed
        } else if (RoleName.MANAGER.name().equals(role) && managerCanView) {
            // MANAGER allowed when configured
        } else {
            throw new AccessDeniedBusinessException("AUDIT_VIEW_DENIED", "Bạn không có quyền xem nhật ký kiểm toán hệ thống.");
        }

        int boundedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (outcome != null) {
                predicates.add(cb.equal(root.get("outcome"), outcome));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> logsPage = auditLogRepository.findAll(spec, pageable);
        List<AuditLogResponse> items = logsPage.getContent().stream()
                .map(AuditLogResponse::fromEntity)
                .toList();

        auditService.record(
                "AUDIT_VIEW",
                "AUDIT_LOG",
                "PAGE_" + page,
                AuditOutcome.SUCCESS,
                "SUCCESS",
                null,
                Map.of("page", page, "size", boundedSize, "total", logsPage.getTotalElements())
        );

        return PageResponse.<AuditLogResponse>builder()
                .items(items)
                .page(logsPage.getNumber())
                .size(logsPage.getSize())
                .total(logsPage.getTotalElements())
                .build();
    }
}

