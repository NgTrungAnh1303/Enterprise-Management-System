package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.dashboard.DashboardResponse;
import com.fpt.ems.be.model.MaintenancePlan;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.model.enums.IncidentStatus;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.IncidentReportRepository;
import com.fpt.ems.be.repository.MaintenancePlanRepository;
import com.fpt.ems.be.util.DueLabelCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final MaintenancePlanRepository planRepository;
    private final IncidentReportRepository incidentRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Map<EquipmentStatus, Long> equipmentByStatus = new EnumMap<>(EquipmentStatus.class);
        for (EquipmentStatus status : EquipmentStatus.values()) {
            equipmentByStatus.put(status, equipmentRepository.countByStatus(status));
        }

        LocalDate today = LocalDate.now();
        List<MaintenancePlan> plans = planRepository.findAll();
        long overdueCount = 0;
        long dueSoonCount = 0;

        for (MaintenancePlan plan : plans) {
            if (plan.getNextDueDate() == null) continue;
            String label = DueLabelCalculator.calculateDueLabel(plan.getNextDueDate(), today);
            if ("OVERDUE".equals(label)) {
                overdueCount++;
            } else if ("DUE_SOON".equals(label)) {
                dueSoonCount++;
            }
        }

        long openIncidents = incidentRepository.countByStatusNot(IncidentStatus.CLOSED);

        return DashboardResponse.builder()
                .equipmentByStatus(equipmentByStatus)
                .overdueCount(overdueCount)
                .dueSoonCount(dueSoonCount)
                .openIncidents(openIncidents)
                .build();
    }
}

