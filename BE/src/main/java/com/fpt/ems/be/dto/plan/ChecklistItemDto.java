package com.fpt.ems.be.dto.plan;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItemDto {
    private Long checklistId;

    @NotBlank(message = "Hạng mục không được để trống")
    private String item;

    @Builder.Default
    private Boolean isRequired = true;
}

