package com.fpt.ems.be.dto.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fpt.ems.be.model.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class IncidentProgressRequest {

    @NotNull(message = "Trạng thái tiến độ (IN_REPAIR hoặc RESOLVED) là bắt buộc.")
    private IncidentStatus status;

    private String note;
}

