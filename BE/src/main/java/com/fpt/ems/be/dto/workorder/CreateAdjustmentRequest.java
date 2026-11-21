package com.fpt.ems.be.dto.workorder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateAdjustmentRequest {

    @NotBlank(message = "Lý do điều chỉnh là bắt buộc.")
    private String reason;

    private String changeDetail;
}

