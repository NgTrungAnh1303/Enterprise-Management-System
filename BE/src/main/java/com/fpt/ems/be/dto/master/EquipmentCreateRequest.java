package com.fpt.ems.be.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentCreateRequest {
    @NotBlank(message = "Mã thiết bị không được để trống")
    private String code;

    private String brand;
    private String model;
    private String serial;

    @NotNull(message = "Loại thiết bị không được để trống")
    private Long typeId;

    @NotNull(message = "Vị trí không được để trống")
    private Long locationId;
}

