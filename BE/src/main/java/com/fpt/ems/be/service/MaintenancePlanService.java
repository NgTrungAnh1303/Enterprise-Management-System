package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.plan.MaintenancePlanRequest;
import com.fpt.ems.be.dto.plan.MaintenancePlanResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.MaintenancePlan;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.MaintenancePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenancePlanService {

    private static final Set<Integer> ALLOWED_CYCLES = Set.of(30, 90, 180);

    private final MaintenancePlanRepository planRepository;
    private final EquipmentRepository equipmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public MaintenancePlanResponse getPlanByEquipmentId(Long equipmentId) {
        MaintenancePlan plan = planRepository.findByEquipment_EquipmentId(equipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Thiết bị chưa được cấu hình kế hoạch bảo trì."));
        return MaintenancePlanResponse.fromEntity(plan);
    }

    @Transactional
    public MaintenancePlanResponse saveOrUpdatePlan(Long equipmentId, MaintenancePlanRequest request) {
        if (!ALLOWED_CYCLES.contains(request.getCycleDays())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CYCLE_DAYS",
                    "Chu kỳ bảo trì không hợp lệ. Chỉ chấp nhận các chu kỳ 30, 90 hoặc 180 ngày.");
        }

        Equipment equipment = equipmentRepository.findByIdWithLock(equipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        MaintenancePlan plan = planRepository.findByEquipment_EquipmentId(equipmentId).orElse(null);

        Map<String, Object> oldVal = null;
        if (plan != null) {
            oldVal = new HashMap<>();
            oldVal.put("cycleDays", plan.getCycleDays());
            if (plan.getNextDueDate() != null) {
                oldVal.put("nextDueDate", plan.getNextDueDate().toString());
            }
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        if (plan == null) {
            LocalDate nextDue = startDate.plusDays(request.getCycleDays());
            plan = MaintenancePlan.builder()
                    .equipment(equipment)
                    .cycleDays(request.getCycleDays())
                    .lastCompletedDate(null)
                    .nextDueDate(nextDue)
                    .build();
        } else {
            plan.setCycleDays(request.getCycleDays());
            LocalDate anchorDate = plan.getLastCompletedDate() != null ? plan.getLastCompletedDate() : startDate;
            plan.setNextDueDate(anchorDate.plusDays(request.getCycleDays()));
        }

        MaintenancePlan saved = planRepository.save(plan);

        Map<String, Object> newVal = new HashMap<>();
        newVal.put("cycleDays", saved.getCycleDays());
        if (saved.getNextDueDate() != null) {
            newVal.put("nextDueDate", saved.getNextDueDate().toString());
        }

        auditService.record(
                "PLAN_UPDATE",
                "MAINTENANCE_PLAN",
                saved.getPlanId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                oldVal,
                newVal
        );

        return MaintenancePlanResponse.fromEntity(saved);
    }
}

