package com.fpt.ems.be.dto.history;

import com.fpt.ems.be.model.MaintenanceHistory;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceHistoryResponse {
    private Long historyId;
    private Long equipmentId;
    private String equipmentCode;
    private Long woId;
    private String woCode;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String performedByName;
    private Instant performedAt;
    private String note;

    public static MaintenanceHistoryResponse fromEntity(MaintenanceHistory h) {
        if (h == null) return null;
        return MaintenanceHistoryResponse.builder()
                .historyId(h.getHistoryId())
                .equipmentId(h.getEquipment() != null ? h.getEquipment().getEquipmentId() : null)
                .equipmentCode(h.getEquipment() != null ? h.getEquipment().getCode() : null)
                .woId(h.getWorkOrder() != null ? h.getWorkOrder().getWoId() : null)
                .woCode(h.getWorkOrder() != null ? h.getWorkOrder().getWoCode() : null)
                .action(h.getAction())
                .oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus())
                .performedByName(h.getPerformedBy() != null ? h.getPerformedBy().getFullName() : "SYSTEM")
                .performedAt(h.getPerformedAt())
                .note(h.getNote())
                .build();
    }
}

