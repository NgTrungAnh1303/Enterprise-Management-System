package com.fpt.ems.be.dto.statusrequest;

import com.fpt.ems.be.model.StatusChangeRequest;
import com.fpt.ems.be.model.enums.StatusRequestStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeRequestResponse {
    private Long requestId;
    private Long equipmentId;
    private String equipmentCode;
    private Long woId;
    private String woCode;
    private String proposedStatus;
    private String reason;
    private StatusRequestStatus status;
    private Long proposedById;
    private String proposedByName;
    private Long decidedById;
    private String decidedByName;
    private Instant proposedAt;
    private Instant decidedAt;

    public static StatusChangeRequestResponse fromEntity(StatusChangeRequest req) {
        if (req == null) return null;
        return StatusChangeRequestResponse.builder()
                .requestId(req.getRequestId())
                .equipmentId(req.getEquipment() != null ? req.getEquipment().getEquipmentId() : null)
                .equipmentCode(req.getEquipment() != null ? req.getEquipment().getCode() : null)
                .woId(req.getWorkOrder() != null ? req.getWorkOrder().getWoId() : null)
                .woCode(req.getWorkOrder() != null ? req.getWorkOrder().getWoCode() : null)
                .proposedStatus(req.getProposedStatus())
                .reason(req.getReason())
                .status(req.getStatus())
                .proposedById(req.getProposedBy() != null ? req.getProposedBy().getUserId() : null)
                .proposedByName(req.getProposedBy() != null ? req.getProposedBy().getFullName() : null)
                .decidedById(req.getDecidedBy() != null ? req.getDecidedBy().getUserId() : null)
                .decidedByName(req.getDecidedBy() != null ? req.getDecidedBy().getFullName() : null)
                .proposedAt(req.getProposedAt())
                .decidedAt(req.getDecidedAt())
                .build();
    }
}

