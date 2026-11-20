package com.fpt.ems.be.dto.plan;

import com.fpt.ems.be.model.MaintenancePlan;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlanResponse {
    private Long planId;
    private Long equipmentId;
    private Integer cycleDays;
    private LocalDate lastCompletedDate;
    private LocalDate nextDueDate;
    private Instant createdAt;

    public static MaintenancePlanResponse fromEntity(MaintenancePlan plan) {
        if (plan == null) return null;
        return MaintenancePlanResponse.builder()
                .planId(plan.getPlanId())
                .equipmentId(plan.getEquipment().getEquipmentId())
                .cycleDays(plan.getCycleDays())
                .lastCompletedDate(plan.getLastCompletedDate())
                .nextDueDate(plan.getNextDueDate())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}

