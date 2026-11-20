package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.plan.MaintenancePlanRequest;
import com.fpt.ems.be.dto.plan.MaintenancePlanResponse;
import com.fpt.ems.be.service.MaintenancePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/equipment/{id}/plan")
@RequiredArgsConstructor
@Tag(name = "Maintenance Plan", description = "Cấu hình kế hoạch bảo trì định kỳ (API-13)")
public class MaintenancePlanController {

    private final MaintenancePlanService planService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Xem kế hoạch bảo trì của thiết bị (API-13: MANAGER, TECHNICIAN)")
    public ResponseEntity<MaintenancePlanResponse> getPlan(@PathVariable("id") Long equipmentId) {
        return ResponseEntity.ok(planService.getPlanByEquipmentId(equipmentId));
    }

    @PutMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Thiết lập/cập nhật kế hoạch bảo trì (API-13: Chỉ MANAGER)")
    public ResponseEntity<MaintenancePlanResponse> updatePlan(
            @PathVariable("id") Long equipmentId,
            @Valid @RequestBody MaintenancePlanRequest request) {
        return ResponseEntity.ok(planService.saveOrUpdatePlan(equipmentId, request));
    }
}

