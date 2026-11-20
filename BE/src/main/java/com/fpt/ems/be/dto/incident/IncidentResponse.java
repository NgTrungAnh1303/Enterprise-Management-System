package com.fpt.ems.be.dto.incident;

import com.fpt.ems.be.model.IncidentReport;
import com.fpt.ems.be.model.enums.IncidentSeverity;
import com.fpt.ems.be.model.enums.IncidentStatus;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {
    private Long incidentId;
    private String trackingCode;
    private Long equipmentId;
    private String equipmentCode;
    private String brand;
    private String model;
    private Long reporterId;
    private String reporterName;
    private String description;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private Instant createdAt;
    private Instant closedAt;

    public static IncidentResponse fromEntity(IncidentReport incident) {
        if (incident == null) return null;
        return IncidentResponse.builder()
                .incidentId(incident.getIncidentId())
                .trackingCode(incident.getTrackingCode())
                .equipmentId(incident.getEquipment() != null ? incident.getEquipment().getEquipmentId() : null)
                .equipmentCode(incident.getEquipment() != null ? incident.getEquipment().getCode() : null)
                .brand(incident.getEquipment() != null ? incident.getEquipment().getBrand() : null)
                .model(incident.getEquipment() != null ? incident.getEquipment().getModel() : null)
                .reporterId(incident.getReporter() != null ? incident.getReporter().getUserId() : null)
                .reporterName(incident.getReporter() != null ? incident.getReporter().getFullName() : null)
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .closedAt(incident.getClosedAt())
                .build();
    }
}

