package com.fpt.ems.be.security;

import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.IncidentReport;
import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.WorkOrderAssignment;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.IncidentReportRepository;
import com.fpt.ems.be.repository.InspectionResultRepository;
import com.fpt.ems.be.repository.MaintenanceWorkOrderRepository;
import com.fpt.ems.be.repository.WorkOrderAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationPolicy {

    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository assignmentRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final AppUserRepository userRepository;

    /**
     * 
     */
    public void assertTechnicianAssigned(Long woId, Long technicianId) {
        MaintenanceWorkOrder wo = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại"));

        if (wo.getStatus() == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE", "Work order đã đóng, không thể sửa đổi.");
        }

        Optional<WorkOrderAssignment> currentAssignmentOpt = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(woId);
        if (currentAssignmentOpt.isEmpty() || !currentAssignmentOpt.get().getTechnician().getUserId().equals(technicianId)) {
            throw new AccessDeniedBusinessException("WO_NOT_ASSIGNED", "Bạn không được phân công work order này.", "WORK_ORDER", String.valueOf(woId));
        }
    }

    /**
     * 
     */
    public void assertCanAssignTechnician(Long technicianId) {
        AppUser tech = userRepository.findById(technicianId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Technician không tồn tại"));

        if (tech.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TECHNICIAN_NOT_ACTIVE", "Technician đang bị khóa hoặc ngừng hoạt động.");
        }

        if (tech.getRole().getName() != RoleName.TECHNICIAN) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TECHNICIAN_ROLE", "Người dùng được chọn không phải là Technician.");
        }
    }

    /**
     * 
     */
    public void assertNotSelfAcceptance(Long woId, Long managerUserId) {
        // 1. Check all assignments
        List<WorkOrderAssignment> assignments = assignmentRepository.findByWorkOrder_WoId(woId);
        for (WorkOrderAssignment assignment : assignments) {
            if (assignment.getTechnician().getUserId().equals(managerUserId)) {
                throw new AccessDeniedBusinessException("SELF_ACCEPT", "Manager không được tự nghiệm thu work order do chính mình từng thực hiện.");
            }
        }

        // 2. Check all inspection results entered_by
        if (inspectionResultRepository.existsByWorkOrder_WoIdAndEnteredBy_UserId(woId, managerUserId)) {
            throw new AccessDeniedBusinessException("SELF_ACCEPT", "Manager không được tự nghiệm thu work order do chính mình từng nhập kết quả.");
        }
    }

    /**
     * 
     */
    public void assertWorkOrderNotClosed(Long woId) {
        MaintenanceWorkOrder wo = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại"));

        if (wo.getStatus() == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE", "Work order đã đóng, không thể sửa đổi.");
        }
    }

    /**
     * 
     */
    public void assertCanReadWorkOrder(Long woId, Long currentUserId, String currentRole) {
        if (RoleName.MANAGER.name().equals(currentRole)) {
            return;
        }
        if (RoleName.TECHNICIAN.name().equals(currentRole)) {
            if (!assignmentRepository.existsByWorkOrder_WoIdAndTechnician_UserId(woId, currentUserId)) {
                throw new AccessDeniedBusinessException("WO_NOT_ASSIGNED", "Bạn không được phân công work order này.");
            }
            return;
        }
        throw new AccessDeniedBusinessException("ACCESS_DENIED", "Bạn không có quyền truy cập work order này.");
    }

    /**
     * 
     */
    public void assertCanReadEquipment(Long equipmentId, Long currentUserId, String currentRole) {
        if (RoleName.MANAGER.name().equals(currentRole) || RoleName.ADMIN.name().equals(currentRole)
                || RoleName.EQUIPMENT_USER.name().equals(currentRole)) {
            return;
        }
        if (RoleName.TECHNICIAN.name().equals(currentRole)) {
            if (!assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(equipmentId, currentUserId)) {
                throw new AccessDeniedBusinessException("ACCESS_DENIED", "Technician chỉ có quyền xem thiết bị liên quan công việc được giao.");
            }
            return;
        }
        throw new AccessDeniedBusinessException("ACCESS_DENIED", "Bạn không có quyền xem thiết bị này.");
    }

    /**
     * 
     */
    public void assertCanReadEquipmentHistory(Long equipmentId, Long currentUserId, String currentRole) {
        if (RoleName.MANAGER.name().equals(currentRole)) {
            return;
        }
        if (RoleName.TECHNICIAN.name().equals(currentRole)) {
            if (!assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(equipmentId, currentUserId)) {
                throw new AccessDeniedBusinessException("ACCESS_DENIED", "Technician chỉ có quyền xem lịch sử thiết bị liên quan công việc được giao.");
            }
            return;
        }
        throw new AccessDeniedBusinessException("ACCESS_DENIED", "Bạn không có quyền xem lịch sử thiết bị.");
    }

    /**
     * 
     */
    public void assertIncidentVisible(Long incidentId, Long currentUserId, String currentRole) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại"));

        if (RoleName.MANAGER.name().equals(currentRole)) {
            return;
        }
        if (RoleName.EQUIPMENT_USER.name().equals(currentRole)) {
            if (!incident.getReporter().getUserId().equals(currentUserId)) {
                throw new AccessDeniedBusinessException("INCIDENT_NOT_OWNED", "Bạn chỉ có thể xem sự cố do chính mình báo cáo.");
            }
            return;
        }
        if (RoleName.TECHNICIAN.name().equals(currentRole)) {
            if (!assignmentRepository.existsByWorkOrder_Incident_IncidentIdAndTechnician_UserId(incidentId, currentUserId)) {
                throw new AccessDeniedBusinessException("ACCESS_DENIED", "Technician chỉ có quyền xem sự cố được phân công xử lý.");
            }
            return;
        }
        throw new AccessDeniedBusinessException("ACCESS_DENIED", "Admin không có quyền truy cập sự cố.");
    }
}
