package com.fpt.ems.be.dto.dashboard;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private Long equipmentId;
    private String equipmentCode;
    private String brand;
    private String model;
    private LocalDate nextDueDate;
    private String dueLabel;
}

