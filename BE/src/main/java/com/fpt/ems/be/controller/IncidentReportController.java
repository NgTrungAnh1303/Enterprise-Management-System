package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.incident.ClassifyIncidentRequest;
import com.fpt.ems.be.dto.incident.IncidentCreateRequest;
import com.fpt.ems.be.dto.incident.IncidentProgressRequest;
import com.fpt.ems.be.dto.incident.IncidentResponse;
import com.fpt.ems.be.model.enums.IncidentSeverity;
import com.fpt.ems.be.model.enums.IncidentStatus;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.IncidentReportService;
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
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Quản lý báo cáo & xử lý sự cố thiết bị (API-24..28)")
public class IncidentReportController {

    private final IncidentReportService incidentService;

    @PostMapping
    @PreAuthorize("hasRole('EQUIPMENT_USER')")
    @Operation(summary = "Báo cáo sự cố mới (API-24: Chỉ EQUIPMENT_USER)")
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody IncidentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.createIncident(
                        request, principal != null ? principal.getUserId() : null
                ));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách sự cố (API-25: USER xem của mình, MANAGER xem tất cả, TECH xem sự cố được giao)")
    public ResponseEntity<PageResponse<IncidentResponse>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.getIncidents(
                status, severity, equipmentId, page, size,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết sự cố (API-25: Theo quy tắc phân quyền AZ-07)")
    public ResponseEntity<IncidentResponse> getIncidentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.getIncidentById(
                id,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @PostMapping("/{id}/classify")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Phân loại mức độ nghiêm trọng sự cố (API-26: Chỉ MANAGER)")
    public ResponseEntity<IncidentResponse> classifyIncident(
            @PathVariable Long id,
            @Valid @RequestBody ClassifyIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.classifyIncident(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật tiến độ xử lý sự cố (API-27: Chỉ TECHNICIAN được giao sửa chữa)")
    public ResponseEntity<IncidentResponse> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody IncidentProgressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.updateProgress(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Đóng sự cố sau khi sửa chữa nghiệm thu ĐẠT (API-28: Chỉ MANAGER)")
    public ResponseEntity<IncidentResponse> closeIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.closeIncident(
                id, principal != null ? principal.getUserId() : null
        ));
    }
}

