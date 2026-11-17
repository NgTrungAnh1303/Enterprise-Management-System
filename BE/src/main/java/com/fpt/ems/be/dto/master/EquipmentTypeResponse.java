package com.fpt.ems.be.dto.master;

import com.fpt.ems.be.model.EquipmentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentTypeResponse {
    private Long typeId;
    private String name;
    private String description;

    public static EquipmentTypeResponse fromEntity(EquipmentType type) {
        if (type == null) return null;
        return EquipmentTypeResponse.builder()
                .typeId(type.getTypeId())
                .name(type.getName())
                .description(type.getDescription())
                .build();
    }
}

