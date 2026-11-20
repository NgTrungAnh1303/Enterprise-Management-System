package com.fpt.ems.be.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.AuditLogRepository;
import com.fpt.ems.be.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_AUDIT_FIELDS = Set.of(
            "userId", "username", "fullName", "status", "roleId", "roleName",
            "equipmentId", "equipmentCode", "decision", "note", "reason",
            "planId", "incidentId", "workOrderId", "action", "detail", "item",
            "period", "woId", "woCode", "technicianId", "oldStatus", "newStatus",
            "severity", "cycleDays", "nextDueDate", "trackingCode", "missingItems",
            "details", "path", "requestId", "adjustmentId", "itemCount", "proposedStatus"
    );

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog record(String action, String targetType, String targetId,
                           AuditOutcome outcome, String reasonCode,
                           Object oldValue, Object newValue) {
        return saveAuditLog(action, targetType, targetId, outcome, reasonCode, oldValue, newValue, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog recordWithActor(String action, String targetType, String targetId,
                                    AuditOutcome outcome, String reasonCode,
                                    Object oldValue, Object newValue,
                                    Long actorId, String actorRole) {
        return saveAuditLog(action, targetType, targetId, outcome, reasonCode, oldValue, newValue, actorId, actorRole);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordDenied(String action, String targetType, String targetId,
                                 AuditOutcome outcome, String reasonCode,
                                 Object oldValue, Object newValue) {
        return saveAuditLog(action, targetType, targetId, outcome, reasonCode, oldValue, newValue, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordDeniedWithActor(String action, String targetType, String targetId,
                                          AuditOutcome outcome, String reasonCode,
                                          Object oldValue, Object newValue,
                                          Long actorId, String actorRole) {
        return saveAuditLog(action, targetType, targetId, outcome, reasonCode, oldValue, newValue, actorId, actorRole);
    }

    private AuditLog saveAuditLog(String action, String targetType, String targetId,
                                  AuditOutcome outcome, String reasonCode,
                                  Object oldValue, Object newValue,
                                  Long explicitActorId, String explicitActorRole) {
        Long actorId = explicitActorId;
        String actorRole = explicitActorRole;

        if (actorId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                actorId = principal.getUserId();
                actorRole = principal.getRole();
            } else if (actorRole == null) {
                actorRole = "SYSTEM";
            }
        }

        String requestId = MDC.get("requestId");
        String sourceIp = resolveClientIp();

        String sanitizedOldJson = sanitizeToJson(oldValue);
        String sanitizedNewJson = sanitizeToJson(newValue);

        AuditLog auditLog = AuditLog.builder()
                .occurredAt(Instant.now())
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .outcome(outcome)
                .reasonCode(reasonCode)
                .oldValue(sanitizedOldJson)
                .newValue(sanitizedNewJson)
                .requestId(requestId)
                .sourceIp(sourceIp)
                .build();

        return auditLogRepository.save(auditLog);
    }

    private String sanitizeToJson(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            Map<String, Object> map;
            if (payload instanceof Map<?, ?> rawMap) {
                map = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() != null && ALLOWED_AUDIT_FIELDS.contains(entry.getKey().toString())) {
                        map.put(entry.getKey().toString(), entry.getValue());
                    }
                }
            } else {
                Map<String, Object> converted = objectMapper.convertValue(payload, new TypeReference<>() {});
                map = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : converted.entrySet()) {
                    if (ALLOWED_AUDIT_FIELDS.contains(entry.getKey())) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (map.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize audit payload: {}", e.getMessage());
            return null;
        }
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "UNKNOWN";
        }
        HttpServletRequest request = attrs.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "UNKNOWN";
    }
}
