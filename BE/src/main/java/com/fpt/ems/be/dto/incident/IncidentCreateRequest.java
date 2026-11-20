package com.fpt.ems.be.dto.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class IncidentCreateRequest {

    @NotNull(message = "ID thiết bị là bắt buộc.")
    private Long equipmentId;

    @NotBlank(message = "Mô tả sự cố là bắt buộc.")
    private String description;
}

