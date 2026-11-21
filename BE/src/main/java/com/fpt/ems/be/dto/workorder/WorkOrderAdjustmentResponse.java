package com.fpt.ems.be.dto.workorder;

import com.fpt.ems.be.model.WorkOrderAdjustment;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderAdjustmentResponse {
    private Long adjustmentId;
    private Long woId;
    private String reason;
    private String changeDetail;
    private Long createdById;
    private String createdByName;
    private Instant createdAt;

    public static WorkOrderAdjustmentResponse fromEntity(WorkOrderAdjustment adjustment) {
        if (adjustment == null) return null;
        return WorkOrderAdjustmentResponse.builder()
                .adjustmentId(adjustment.getAdjustmentId())
                .woId(adjustment.getWorkOrder() != null ? adjustment.getWorkOrder().getWoId() : null)
                .reason(adjustment.getReason())
                .changeDetail(adjustment.getChangeDetail())
                .createdById(adjustment.getCreatedBy() != null ? adjustment.getCreatedBy().getUserId() : null)
                .createdByName(adjustment.getCreatedBy() != null ? adjustment.getCreatedBy().getFullName() : null)
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}

