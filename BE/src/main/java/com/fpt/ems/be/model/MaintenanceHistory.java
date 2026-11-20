package com.fpt.ems.be.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "maintenance_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wo_id")
    private MaintenanceWorkOrder workOrder;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private AppUser performedBy;

    @Column(name = "performed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant performedAt = Instant.now();

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}

