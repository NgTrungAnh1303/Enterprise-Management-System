package com.fpt.ems.be.dto.master;

import com.fpt.ems.be.dto.statusrequest.StatusChangeRequestResponse;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.util.DueLabelCalculator;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponse {
    private Long id;
    private Long equipmentId;
    private String code;
    private String brand;
    private String model;
    private String serial;
    private EquipmentStatus status;
    private EquipmentTypeResponse type;
    private LocationResponse location;
    private String dueLabel;
    private LocalDate nextDueDate;
    private StatusChangeRequestResponse pendingStatusRequest;
    private Instant createdAt;

    public static EquipmentResponse fromEntity(Equipment equipment, LocalDate nextDueDate) {
        if (equipment == null) return null;
        String label = DueLabelCalculator.calculate(nextDueDate);
        return EquipmentResponse.builder()
                .id(equipment.getEquipmentId())
                .equipmentId(equipment.getEquipmentId())
                .code(equipment.getCode())
                .brand(equipment.getBrand())
                .model(equipment.getModel())
                .serial(equipment.getSerial())
                .status(equipment.getStatus())
                .type(EquipmentTypeResponse.fromEntity(equipment.getType()))
                .location(LocationResponse.fromEntity(equipment.getLocation()))
                .dueLabel(label)
                .nextDueDate(nextDueDate)
                .createdAt(equipment.getCreatedAt())
                .build();
    }
}

