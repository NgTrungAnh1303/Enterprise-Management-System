package com.fpt.ems.be.dto.workorder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fpt.ems.be.model.enums.AcceptanceDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class AcceptWorkOrderRequest {

    @NotNull(message = "Quyết định nghiệm thu (PASS/FAIL) là bắt buộc.")
    private AcceptanceDecision decision;

    private String note;
}

