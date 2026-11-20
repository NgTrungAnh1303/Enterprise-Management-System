package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.audit.AuditLogResponse;
import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Audit Log", description = "Endpoints xem nhật ký kiểm toán và an toàn hệ thống (Chỉ đọc, Append-only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(
            summary = "Tra cứu nhật ký kiểm toán hệ thống",
            description = "Tra cứu danh sách audit log có phân trang và lọc theo actor, action, outcome, khoảng thời gian. Dành cho ADMIN và MANAGER (nếu được cấp quyền qua cấu hình).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền xem nhật ký kiểm toán")
            }
    )
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PageResponse<AuditLogResponse> response = auditLogService.getAuditLogs(
                principal, actorId, action, outcome, from, to, page, size
        );
        return ResponseEntity.ok(response);
    }
}

