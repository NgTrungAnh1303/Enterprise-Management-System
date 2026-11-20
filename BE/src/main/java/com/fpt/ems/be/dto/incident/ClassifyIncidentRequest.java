package com.fpt.ems.be.dto.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fpt.ems.be.model.enums.IncidentSeverity;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClassifyIncidentRequest {

    @NotNull(message = "Mức độ nghiêm trọng (LOW, MEDIUM, HIGH, CRITICAL) là bắt buộc.")
    private IncidentSeverity severity;
}

