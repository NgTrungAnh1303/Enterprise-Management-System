package com.fpt.ems.be.dto.statusrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateStatusRequestDto {

    @Builder.Default
    private String proposedStatus = "OUT_OF_SERVICE";

    @NotBlank(message = "Lý do đề xuất ngừng hoạt động là bắt buộc.")
    private String reason;

    private Long woId;
}

