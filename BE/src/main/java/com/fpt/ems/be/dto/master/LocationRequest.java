package com.fpt.ems.be.dto.master;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {
    @NotBlank(message = "Phòng (room) không được để trống")
    private String room;

    @NotBlank(message = "Khu vực (area) không được để trống")
    private String area;

    private String department;
}

