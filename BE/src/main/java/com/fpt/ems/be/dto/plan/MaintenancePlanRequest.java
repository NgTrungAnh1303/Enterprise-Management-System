package com.fpt.ems.be.dto.plan;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlanRequest {
    @NotNull(message = "Chu kỳ bảo trì không được để trống")
    private Integer cycleDays;

    private LocalDate startDate;
}

