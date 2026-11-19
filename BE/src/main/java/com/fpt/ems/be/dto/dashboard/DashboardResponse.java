package com.fpt.ems.be.dto.dashboard;

import com.fpt.ems.be.model.enums.EquipmentStatus;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Map<EquipmentStatus, Long> equipmentByStatus;
    private long overdueCount;
    private long dueSoonCount;
    private long openIncidents;
}

