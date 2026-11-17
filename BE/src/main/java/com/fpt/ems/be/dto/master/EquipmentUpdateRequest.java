package com.fpt.ems.be.dto.master;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentUpdateRequest {
    private String brand;
    private String model;
    private String serial;

    @NotNull(message = "Loại thiết bị không được để trống")
    private Long typeId;

    @NotNull(message = "Vị trí không được để trống")
    private Long locationId;
}

