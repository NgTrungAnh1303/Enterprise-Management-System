package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.statusrequest.CreateStatusRequestDto;
import com.fpt.ems.be.dto.statusrequest.StatusChangeRequestResponse;
import com.fpt.ems.be.dto.statusrequest.StatusRequestDecisionDto;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.StatusChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Status Change Requests", description = "Đề xuất và phê duyệt ngừng hoạt động thiết bị Out of Service (API-11, API-12)")
public class StatusChangeRequestController {

    private final StatusChangeRequestService statusChangeRequestService;

    @PostMapping("/equipment/{id}/status-requests")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Đề xuất ngừng hoạt động thiết bị OUT_OF_SERVICE (API-11: Chỉ TECHNICIAN có WO được giao trên thiết bị)")
    public ResponseEntity<StatusChangeRequestResponse> proposeOutOfService(
            @PathVariable Long id,
            @Valid @RequestBody CreateStatusRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statusChangeRequestService.proposeOutOfService(
                        id, request, principal != null ? principal.getUserId() : null
                ));
    }

    @GetMapping("/equipment/{id}/status-requests")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Xem lịch sử đề xuất thay đổi trạng thái của thiết bị (Chỉ MANAGER)")
    public ResponseEntity<List<StatusChangeRequestResponse>> getRequestsByEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(statusChangeRequestService.getRequestsByEquipment(id));
    }

    @PostMapping("/status-requests/{id}/decision")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Xác nhận hoặc từ chối đề xuất ngừng hoạt động (API-12: Chỉ MANAGER)")
    public ResponseEntity<StatusChangeRequestResponse> decideStatusRequest(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequestDecisionDto request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(statusChangeRequestService.decideStatusRequest(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @GetMapping("/status-requests")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Xem tất cả đề xuất thay đổi trạng thái (Chỉ MANAGER)")
    public ResponseEntity<List<StatusChangeRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(statusChangeRequestService.getAllRequests());
    }
}

