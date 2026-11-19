package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.dashboard.AlertResponse;
import com.fpt.ems.be.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Cảnh báo bảo trì định kỳ Đến hạn & Quá hạn (API-30)")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Lấy danh sách thiết bị đến hạn / quá hạn bảo trì (API-30: Chỉ MANAGER)")
    public ResponseEntity<List<AlertResponse>> getAlerts() {
        return ResponseEntity.ok(alertService.getAlerts());
    }
}

