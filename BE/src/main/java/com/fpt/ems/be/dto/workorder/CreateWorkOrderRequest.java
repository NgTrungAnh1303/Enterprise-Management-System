package com.fpt.ems.be.dto.workorder;

import com.fpt.ems.be.model.enums.WorkOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkOrderRequest {
    @NotNull(message = "Thiết bị không được để trống")
    private Long equipmentId;

    @NotNull(message = "Loại work order không được để trống")
    private WorkOrderType type;

    
    private String period;

    
    private Long incidentId;
}

