package com.fpt.ems.be.model;

import com.fpt.ems.be.model.enums.StatusRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "status_change_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wo_id")
    private MaintenanceWorkOrder workOrder;

    @Column(name = "proposed_status", nullable = false, length = 50)
    @Builder.Default
    private String proposedStatus = "OUT_OF_SERVICE";

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusRequestStatus status = StatusRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by", nullable = false)
    private AppUser proposedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private AppUser decidedBy;

    @Column(name = "proposed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant proposedAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;
}

