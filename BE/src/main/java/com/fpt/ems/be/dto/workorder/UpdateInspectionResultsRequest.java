package com.fpt.ems.be.dto.workorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInspectionResultsRequest {
    @NotEmpty(message = "Danh sách kết quả kiểm tra không được để trống")
    @Valid
    private List<InspectionResultItemDto> items;
}

