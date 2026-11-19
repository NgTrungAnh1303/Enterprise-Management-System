package com.fpt.ems.be.dto.audit;

import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.enums.AuditOutcome;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long logId;
    private Instant occurredAt;
    private Long actorId;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private AuditOutcome outcome;
    private String reasonCode;
    private String oldValue;
    private String newValue;
    private String requestId;
    private String sourceIp;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .occurredAt(log.getOccurredAt())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .outcome(log.getOutcome())
                .reasonCode(log.getReasonCode())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .requestId(log.getRequestId())
                .sourceIp(log.getSourceIp())
                .build();
    }
}

