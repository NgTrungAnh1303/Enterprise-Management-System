package com.fpt.ems.be.dto.workorder;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionResultItemDto {
    @NotNull(message = "checklistId không được để trống")
    private Long checklistId;

    @Builder.Default
    private Boolean done = false;

    private String condition;
    private String defect;
    private String workDone;
}

