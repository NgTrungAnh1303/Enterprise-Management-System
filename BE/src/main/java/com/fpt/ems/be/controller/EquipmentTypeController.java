package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.master.EquipmentTypeRequest;
import com.fpt.ems.be.dto.master.EquipmentTypeResponse;
import com.fpt.ems.be.service.EquipmentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment-types")
@RequiredArgsConstructor
@Tag(name = "Equipment Types", description = "Quản lý danh mục loại thiết bị (API-10)")
public class EquipmentTypeController {

    private final EquipmentTypeService typeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách loại thiết bị")
    public ResponseEntity<List<EquipmentTypeResponse>> getAllTypes() {
        return ResponseEntity.ok(typeService.getAllTypes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết loại thiết bị theo ID")
    public ResponseEntity<EquipmentTypeResponse> getTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(typeService.getTypeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo loại thiết bị mới (ADMIN, MANAGER)")
    public ResponseEntity<EquipmentTypeResponse> createType(@Valid @RequestBody EquipmentTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(typeService.createType(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật loại thiết bị (ADMIN, MANAGER)")
    public ResponseEntity<EquipmentTypeResponse> updateType(@PathVariable Long id,
                                                           @Valid @RequestBody EquipmentTypeRequest request) {
        return ResponseEntity.ok(typeService.updateType(id, request));
    }
}

