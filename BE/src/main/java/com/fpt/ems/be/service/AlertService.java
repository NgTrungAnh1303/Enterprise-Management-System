package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.dashboard.AlertResponse;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.MaintenancePlan;
import com.fpt.ems.be.repository.MaintenancePlanRepository;
import com.fpt.ems.be.util.DueLabelCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final MaintenancePlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts() {
        LocalDate today = LocalDate.now();
        List<MaintenancePlan> plans = planRepository.findAll();
        List<AlertResponse> alerts = new ArrayList<>();

        for (MaintenancePlan plan : plans) {
            if (plan.getNextDueDate() == null) continue;
            String label = DueLabelCalculator.calculateDueLabel(plan.getNextDueDate(), today);
            if ("OVERDUE".equals(label) || "DUE_SOON".equals(label)) {
                Equipment eq = plan.getEquipment();
                alerts.add(AlertResponse.builder()
                        .equipmentId(eq.getEquipmentId())
                        .equipmentCode(eq.getCode())
                        .brand(eq.getBrand())
                        .model(eq.getModel())
                        .nextDueDate(plan.getNextDueDate())
                        .dueLabel(label)
                        .build());
            }
        }

        return alerts;
    }
}

