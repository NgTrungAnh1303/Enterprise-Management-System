package com.fpt.ems.be.dto.workorder;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignWorkOrderRequest {
    @NotNull(message = "Technician không được để trống")
    private Long technicianId;
}

