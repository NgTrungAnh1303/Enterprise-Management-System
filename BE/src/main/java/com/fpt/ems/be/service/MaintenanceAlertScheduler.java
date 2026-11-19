package com.fpt.ems.be.service;

import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.MaintenancePlan;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.MaintenancePlanRepository;
import com.fpt.ems.be.util.DueLabelCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceAlertScheduler {

    private final EquipmentRepository equipmentRepository;
    private final MaintenancePlanRepository planRepository;
    private final AuditService auditService;

    /**
     * Run daily maintenance alert checks (default at 08:00 daily).
     * Automatically logs DATA_WARNING as actor SYSTEM (actor_id null).
     */
    @Scheduled(cron = "${app.maintenance.alert-cron:0 0 8 * * ?}")
    public void checkMaintenanceDueStatus() {
        log.info("Running daily maintenance due check scheduler...");
        LocalDate today = LocalDate.now();

        List<Equipment> allEquipments = equipmentRepository.findAll();
        for (Equipment eq : allEquipments) {
            MaintenancePlan plan = planRepository.findByEquipment_EquipmentId(eq.getEquipmentId()).orElse(null);
            if (plan == null) {
                Map<String, Object> details = new HashMap<>();
                details.put("equipmentId", eq.getEquipmentId());
                details.put("equipmentCode", eq.getCode());
                details.put("reason", "Thiết bị chưa được cấu hình kế hoạch bảo trì");

                auditService.recordWithActor(
                        "DATA_WARNING",
                        "EQUIPMENT",
                        eq.getEquipmentId().toString(),
                        AuditOutcome.ALERT,
                        "MISSING_PLAN",
                        null,
                        details,
                        null,
                        "SYSTEM"
                );
            } else if (plan.getNextDueDate() != null) {
                String label = DueLabelCalculator.calculateDueLabel(plan.getNextDueDate(), today);
                if ("OVERDUE".equals(label)) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("equipmentId", eq.getEquipmentId());
                    details.put("equipmentCode", eq.getCode());
                    details.put("nextDueDate", plan.getNextDueDate().toString());
                    details.put("reason", "Thiết bị quá hạn bảo trì");

                    auditService.recordWithActor(
                            "DATA_WARNING",
                            "EQUIPMENT",
                            eq.getEquipmentId().toString(),
                            AuditOutcome.ALERT,
                            "OVERDUE_MAINTENANCE",
                            null,
                            details,
                            null,
                            "SYSTEM"
                    );
                }
            }
        }
    }
}

