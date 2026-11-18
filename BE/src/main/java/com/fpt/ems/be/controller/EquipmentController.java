package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.master.EquipmentCreateRequest;
import com.fpt.ems.be.dto.master.EquipmentResponse;
import com.fpt.ems.be.dto.master.EquipmentUpdateRequest;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "Quản lý danh mục thiết bị (API-06, API-07, API-08)")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & phân trang danh sách thiết bị (API-06)")
    public ResponseEntity<PageResponse<EquipmentResponse>> getEquipments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(equipmentService.getEquipments(
                q, typeId, locationId, status, page, size,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết thiết bị theo ID (API-08)")
    public ResponseEntity<EquipmentResponse> getEquipmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(
                id,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo mới thiết bị (API-07: ADMIN, MANAGER)")
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.createEquipment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thiết bị (API-08: ADMIN, MANAGER)")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentUpdateRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, request));
    }
}

