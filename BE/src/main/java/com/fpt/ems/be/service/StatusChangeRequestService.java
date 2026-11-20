package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.statusrequest.CreateStatusRequestDto;
import com.fpt.ems.be.dto.statusrequest.StatusChangeRequestResponse;
import com.fpt.ems.be.dto.statusrequest.StatusRequestDecisionDto;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.StatusChangeRequest;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.model.enums.StatusRequestStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.MaintenanceWorkOrderRepository;
import com.fpt.ems.be.repository.StatusChangeRequestRepository;
import com.fpt.ems.be.repository.WorkOrderAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusChangeRequestService {

    private final StatusChangeRequestRepository statusChangeRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository assignmentRepository;
    private final AppUserRepository userRepository;
    private final MaintenanceHistoryService historyService;
    private final AuditService auditService;

    @Transactional
    public StatusChangeRequestResponse proposeOutOfService(Long equipmentId, CreateStatusRequestDto request, Long technicianId) {
        if (request.getProposedStatus() != null && !"OUT_OF_SERVICE".equalsIgnoreCase(request.getProposedStatus().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PROPOSED_STATUS",
                    "Kỹ thuật viên chỉ có thể đề xuất trạng thái OUT_OF_SERVICE.");
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        
        boolean isAssignedToEquipment = assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(equipmentId, technicianId);
        if (!isAssignedToEquipment) {
            throw new AccessDeniedBusinessException("NOT_ASSIGNED", "Bạn không được phân công công việc trên thiết bị này.");
        }

        
        if (statusChangeRequestRepository.existsByEquipment_EquipmentIdAndStatus(equipmentId, StatusRequestStatus.PENDING)) {
            throw new BusinessException(HttpStatus.CONFLICT, "PENDING_REQUEST_EXISTS",
                    "Đã có đề xuất ngừng hoạt động đang chờ xử lý cho thiết bị này.");
        }

        MaintenanceWorkOrder wo = null;
        if (request.getWoId() != null) {
            wo = workOrderRepository.findById(request.getWoId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));
            if (!wo.getEquipment().getEquipmentId().equals(equipmentId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "WO_EQUIPMENT_MISMATCH", "Work order không thuộc về thiết bị này.");
            }
        }

        AppUser technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Technician không tồn tại."));

        
        StatusChangeRequest statusRequest = StatusChangeRequest.builder()
                .equipment(equipment)
                .workOrder(wo)
                .proposedStatus("OUT_OF_SERVICE")
                .reason(request.getReason())
                .status(StatusRequestStatus.PENDING)
                .proposedBy(technician)
                .proposedAt(Instant.now())
                .build();

        StatusChangeRequest saved = statusChangeRequestRepository.save(statusRequest);

        historyService.record(equipment, wo, "OOS_PROPOSE",
                equipment.getStatus().name(), equipment.getStatus().name(), null,
                "Đề xuất ngừng hoạt động (OUT_OF_SERVICE): " + request.getReason());

        Map<String, Object> details = new HashMap<>();
        details.put("requestId", saved.getRequestId());
        details.put("equipmentCode", equipment.getCode());
        details.put("proposedStatus", "OUT_OF_SERVICE");
        details.put("reason", request.getReason());

        auditService.record(
                "OOS_PROPOSE",
                "EQUIPMENT",
                equipment.getEquipmentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                details
        );

        return StatusChangeRequestResponse.fromEntity(saved);
    }

    @Transactional
    public StatusChangeRequestResponse decideStatusRequest(Long requestId, StatusRequestDecisionDto request, Long managerId) {
        StatusChangeRequest statusRequest = statusChangeRequestRepository.findByIdWithLock(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Yêu cầu thay đổi trạng thái không tồn tại."));

        if (statusRequest.getStatus() != StatusRequestStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    "Đề xuất thay đổi trạng thái này đã được xử lý trước đó (hiện tại: " + statusRequest.getStatus() + ").");
        }

        AppUser manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Manager không tồn tại."));

        Equipment equipment = equipmentRepository.findByIdWithLock(statusRequest.getEquipment().getEquipmentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        if ("CONFIRM".equalsIgnoreCase(request.getDecision())) {
            statusRequest.setStatus(StatusRequestStatus.CONFIRMED);
            statusRequest.setDecidedBy(manager);
            statusRequest.setDecidedAt(Instant.now());
            statusChangeRequestRepository.save(statusRequest);

            EquipmentStatus oldStatus = equipment.getStatus();
            equipment.setStatus(EquipmentStatus.OUT_OF_SERVICE);
            equipmentRepository.save(equipment);

            historyService.record(equipment, statusRequest.getWorkOrder(), "EQUIPMENT_STATUS_CHANGE",
                    oldStatus.name(), EquipmentStatus.OUT_OF_SERVICE.name(), null,
                    "Xác nhận ngừng hoạt động thiết bị: " + (request.getNote() != null ? request.getNote() : ""));

            auditService.record(
                    "EQUIPMENT_STATUS_CHANGE",
                    "EQUIPMENT",
                    equipment.getEquipmentId().toString(),
                    AuditOutcome.SUCCESS,
                    "OK",
                    Map.of("status", oldStatus.name()),
                    Map.of("decision", "CONFIRM", "status", EquipmentStatus.OUT_OF_SERVICE.name(), "requestId", requestId)
            );
        } else {
            statusRequest.setStatus(StatusRequestStatus.REJECTED);
            statusRequest.setDecidedBy(manager);
            statusRequest.setDecidedAt(Instant.now());
            statusChangeRequestRepository.save(statusRequest);

            historyService.record(equipment, statusRequest.getWorkOrder(), "OOS_REJECT",
                    null, null, null,
                    "Từ chối đề xuất ngừng hoạt động: " + (request.getNote() != null ? request.getNote() : ""));

            auditService.record(
                    "OOS_DECISION",
                    "EQUIPMENT",
                    equipment.getEquipmentId().toString(),
                    AuditOutcome.SUCCESS,
                    "OK",
                    null,
                    Map.of("decision", "REJECT", "requestId", requestId, "note", request.getNote() != null ? request.getNote() : "")
            );
        }

        return StatusChangeRequestResponse.fromEntity(statusRequest);
    }

    @Transactional(readOnly = true)
    public List<StatusChangeRequestResponse> getRequestsByEquipment(Long equipmentId) {
        return statusChangeRequestRepository.findByEquipment_EquipmentIdOrderByProposedAtDesc(equipmentId)
                .stream()
                .map(StatusChangeRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusChangeRequestResponse> getAllRequests() {
        return statusChangeRequestRepository.findAllByOrderByProposedAtDesc()
                .stream()
                .map(StatusChangeRequestResponse::fromEntity)
                .toList();
    }
}

