package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.history.MaintenanceHistoryResponse;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.MaintenanceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/equipment/{id}/history")
@RequiredArgsConstructor
@Tag(name = "Maintenance History", description = "Lịch sử bảo trì thiết bị (API-29, Chỉ đọc, append-only)")
public class MaintenanceHistoryController {

    private final MaintenanceHistoryService historyService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem lịch sử bảo trì của thiết bị (API-29: MANAGER xem tất cả, TECHNICIAN chỉ xem thiết bị liên quan)")
    public ResponseEntity<PageResponse<MaintenanceHistoryResponse>> getEquipmentHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(historyService.getEquipmentHistory(
                id, page, size,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }
}

