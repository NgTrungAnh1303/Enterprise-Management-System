package com.fpt.ems.be.dto.statusrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class StatusRequestDecisionDto {

    @NotBlank(message = "Quyết định xử lý là bắt buộc (CONFIRM hoặc REJECT).")
    @Pattern(regexp = "^(CONFIRM|REJECT)$", message = "Quyết định chỉ có thể là CONFIRM hoặc REJECT.")
    private String decision;

    private String note;
}

