package com.fpt.ems.be.model;

import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "maintenance_work_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wo_id")
    private Long woId;

    @Column(name = "wo_code", nullable = false, unique = true, length = 50)
    private String woCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private MaintenancePlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentReport incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WorkOrderType type;

    @Column(name = "period", length = 20)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;
}

