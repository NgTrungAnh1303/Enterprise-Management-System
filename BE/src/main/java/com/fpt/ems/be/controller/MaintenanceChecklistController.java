package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.plan.ChecklistItemDto;
import com.fpt.ems.be.dto.plan.ChecklistUpdateRequest;
import com.fpt.ems.be.service.MaintenanceChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment-types/{id}/checklist")
@RequiredArgsConstructor
@Tag(name = "Maintenance Checklist", description = "Cấu hình checklist kiểm tra theo loại thiết bị (API-14)")
public class MaintenanceChecklistController {

    private final MaintenanceChecklistService checklistService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Xem checklist của loại thiết bị (API-14: MANAGER, TECHNICIAN)")
    public ResponseEntity<List<ChecklistItemDto>> getChecklist(@PathVariable("id") Long typeId) {
        return ResponseEntity.ok(checklistService.getChecklistByTypeId(typeId));
    }

    @PutMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Cập nhật danh sách checklist (API-14: Chỉ MANAGER)")
    public ResponseEntity<List<ChecklistItemDto>> updateChecklist(
            @PathVariable("id") Long typeId,
            @Valid @RequestBody ChecklistUpdateRequest request) {
        return ResponseEntity.ok(checklistService.updateChecklist(typeId, request));
    }
}

