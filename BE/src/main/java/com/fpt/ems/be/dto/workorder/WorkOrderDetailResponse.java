package com.fpt.ems.be.dto.workorder;

import com.fpt.ems.be.dto.master.EquipmentResponse;
import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.AcceptanceDecision;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderDetailResponse {
    private Long woId;
    private String woCode;
    private EquipmentResponse equipment;
    private WorkOrderType type;
    private String period;
    private WorkOrderStatus status;
    private Instant createdAt;
    private Instant closedAt;
    private Long incidentId;
    private String incidentTrackingCode;

    private AssignmentDto currentAssignment;
    private List<InspectionResultDetailDto> inspectionResults;
    private List<AcceptanceHistoryDto> acceptances;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentDto {
        private Long assignmentId;
        private Long technicianId;
        private String technicianName;
        private String assignedByName;
        private Instant assignedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InspectionResultDetailDto {
        private Long checklistId;
        private String item;
        private Boolean isRequired;
        private Long resultId;
        private Boolean done;
        private String condition;
        private String defect;
        private String workDone;
        private String enteredByName;
        private Instant enteredAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AcceptanceHistoryDto {
        private Long acceptanceId;
        private AcceptanceDecision decision;
        private String note;
        private String acceptedByName;
        private Instant acceptedAt;
    }
}

