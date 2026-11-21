package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.WorkOrderService;
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
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders", description = "Quản lý vòng đời Work Order bảo trì & sửa chữa (API-15..21)")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách work order (API-15: MANAGER xem tất cả, TECHNICIAN chỉ xem được giao, USER/ADMIN 403)")
    public ResponseEntity<PageResponse<WorkOrderListResponse>> getWorkOrders(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.getWorkOrders(
                status, equipmentId, page, size,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết work order (API-17: MANAGER xem tất cả, TECHNICIAN chỉ xem được giao)")
    public ResponseEntity<WorkOrderDetailResponse> getWorkOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.getWorkOrderById(
                id,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Tạo mới work order (API-16: Chỉ MANAGER)")
    public ResponseEntity<WorkOrderDetailResponse> createWorkOrder(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderService.createWorkOrder(request, principal != null ? principal.getUserId() : null));
    }

    @PostMapping("/{id}/assignment")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Phân công / phân công lại work order (API-18: Chỉ MANAGER)")
    public ResponseEntity<WorkOrderDetailResponse> assignWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.assignWorkOrder(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Bắt đầu thực hiện work order (API-19: Chỉ TECHNICIAN được giao)")
    public ResponseEntity<WorkOrderDetailResponse> startWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.startWorkOrder(
                id, principal != null ? principal.getUserId() : null
        ));
    }

    @PutMapping("/{id}/results")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật kết quả kiểm tra/checklist (API-20: Chỉ TECHNICIAN được giao)")
    public ResponseEntity<WorkOrderDetailResponse> updateInspectionResults(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInspectionResultsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.updateInspectionResults(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Nộp nghiệm thu work order (API-21: Chỉ TECHNICIAN được giao)")
    public ResponseEntity<WorkOrderDetailResponse> submitWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.submitWorkOrder(
                id, principal != null ? principal.getUserId() : null
        ));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Nghiệm thu work order (API-22: Chỉ MANAGER ≠ người thực hiện)")
    public ResponseEntity<WorkOrderDetailResponse> acceptWorkOrder(
            @PathVariable Long id,
            @Valid @RequestBody AcceptWorkOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.acceptWorkOrder(
                id, request, principal != null ? principal.getUserId() : null
        ));
    }

    @PostMapping("/{id}/adjustments")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Tạo bản ghi điều chỉnh cho WO đã đóng (API-23: Chỉ MANAGER)")
    public ResponseEntity<WorkOrderAdjustmentResponse> createAdjustment(
            @PathVariable Long id,
            @Valid @RequestBody CreateAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderService.createAdjustment(
                        id, request, principal != null ? principal.getUserId() : null
                ));
    }

    @GetMapping("/{id}/adjustments")
    @PreAuthorize("hasAnyRole('MANAGER', 'TECHNICIAN')")
    @Operation(summary = "Lấy danh sách điều chỉnh của work order (API-23: MANAGER và TECHNICIAN được giao; ADMIN, USER, Technician không được giao bị từ chối 403)")
    public ResponseEntity<java.util.List<WorkOrderAdjustmentResponse>> getAdjustments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.getAdjustments(
                id,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getRole() : null
        ));
    }
}

