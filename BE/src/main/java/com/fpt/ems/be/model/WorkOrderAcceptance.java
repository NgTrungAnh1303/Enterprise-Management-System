package com.fpt.ems.be.model;

import com.fpt.ems.be.model.enums.AcceptanceDecision;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "work_order_acceptance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acceptance_id")
    private Long acceptanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wo_id", nullable = false)
    private MaintenanceWorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private AcceptanceDecision decision;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by", nullable = false)
    private AppUser acceptedBy;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant acceptedAt = Instant.now();
}

