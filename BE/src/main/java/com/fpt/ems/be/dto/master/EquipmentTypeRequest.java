package com.fpt.ems.be.dto.master;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentTypeRequest {
    @NotBlank(message = "Tên loại thiết bị không được để trống")
    private String name;

    private String description;
}

