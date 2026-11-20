package com.fpt.ems.be.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "inspection_result", uniqueConstraints = {
    @UniqueConstraint(name = "uq_inspection_result_wo_checklist", columnNames = {"wo_id", "checklist_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wo_id", nullable = false)
    private MaintenanceWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private MaintenanceChecklist checklist;

    @Column(name = "done", nullable = false)
    @Builder.Default
    private Boolean done = false;

    @Column(name = "condition", length = 255)
    private String condition;

    @Column(name = "defect", columnDefinition = "TEXT")
    private String defect;

    @Column(name = "work_done", columnDefinition = "TEXT")
    private String workDone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private AppUser enteredBy;

    @Column(name = "entered_at")
    private Instant enteredAt;
}

