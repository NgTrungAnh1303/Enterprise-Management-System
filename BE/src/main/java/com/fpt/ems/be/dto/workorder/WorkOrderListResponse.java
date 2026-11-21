package com.fpt.ems.be.dto.workorder;

import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.WorkOrderAssignment;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderListResponse {
    private Long woId;
    private String woCode;
    private Long equipmentId;
    private String equipmentCode;
    private WorkOrderType type;
    private String period;
    private WorkOrderStatus status;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private Instant createdAt;
    private Instant closedAt;

    public static WorkOrderListResponse fromEntity(MaintenanceWorkOrder wo, WorkOrderAssignment currentAssignment) {
        if (wo == null) return null;
        return WorkOrderListResponse.builder()
                .woId(wo.getWoId())
                .woCode(wo.getWoCode())
                .equipmentId(wo.getEquipment() != null ? wo.getEquipment().getEquipmentId() : null)
                .equipmentCode(wo.getEquipment() != null ? wo.getEquipment().getCode() : null)
                .type(wo.getType())
                .period(wo.getPeriod())
                .status(wo.getStatus())
                .assignedTechnicianId(currentAssignment != null && currentAssignment.getTechnician() != null
                        ? currentAssignment.getTechnician().getUserId() : null)
                .assignedTechnicianName(currentAssignment != null && currentAssignment.getTechnician() != null
                        ? currentAssignment.getTechnician().getFullName() : null)
                .createdAt(wo.getCreatedAt())
                .closedAt(wo.getClosedAt())
                .build();
    }
}

