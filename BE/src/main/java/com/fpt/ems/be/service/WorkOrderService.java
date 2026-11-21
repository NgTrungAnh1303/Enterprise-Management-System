package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.master.EquipmentResponse;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import com.fpt.ems.be.security.AuthorizationPolicy;
import com.fpt.ems.be.statemachine.EquipmentStateMachine;
import com.fpt.ems.be.statemachine.WorkOrderStateMachine;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository assignmentRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final MaintenanceChecklistRepository checklistRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenancePlanRepository planRepository;
    private final IncidentReportRepository incidentRepository;
    private final AppUserRepository userRepository;
    private final WorkOrderAcceptanceRepository acceptanceRepository;
    private final WorkOrderAdjustmentRepository adjustmentRepository;

    private final WorkOrderStateMachine workOrderStateMachine;
    private final EquipmentStateMachine equipmentStateMachine;
    private final AuthorizationPolicy authorizationPolicy;
    private final MaintenanceHistoryService historyService;
    private final AuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderListResponse> getWorkOrders(WorkOrderStatus status, Long equipmentId,
                                                             int page, int size,
                                                             Long currentUserId, String currentRole) {
        if (RoleName.EQUIPMENT_USER.name().equals(currentRole) || RoleName.ADMIN.name().equals(currentRole)) {
            throw new AccessDeniedBusinessException("ACCESS_DENIED", "Bạn không có quyền xem danh sách work order.");
        }

        if (size > 100) size = 100;
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<MaintenanceWorkOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (equipmentId != null) {
                predicates.add(cb.equal(root.get("equipment").get("equipmentId"), equipmentId));
            }

            if (RoleName.TECHNICIAN.name().equals(currentRole)) {
                var subquery = query.subquery(Long.class);
                var assignRoot = subquery.from(WorkOrderAssignment.class);
                subquery.select(assignRoot.get("workOrder").get("woId"))
                        .where(cb.equal(assignRoot.get("technician").get("userId"), currentUserId));
                predicates.add(root.get("woId").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MaintenanceWorkOrder> pageResult = workOrderRepository.findAll(spec, pageable);

        List<WorkOrderListResponse> items = pageResult.getContent().stream()
                .map(wo -> {
                    WorkOrderAssignment currAssign = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(wo.getWoId()).orElse(null);
                    return WorkOrderListResponse.fromEntity(wo, currAssign);
                })
                .toList();

        return new PageResponse<>(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public WorkOrderDetailResponse getWorkOrderById(Long woId, Long currentUserId, String currentRole) {
        authorizationPolicy.assertCanReadWorkOrder(woId, currentUserId, currentRole);

        MaintenanceWorkOrder wo = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        WorkOrderAssignment currentAssignment = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(woId).orElse(null);

        WorkOrderDetailResponse.AssignmentDto assignmentDto = null;
        if (currentAssignment != null) {
            assignmentDto = WorkOrderDetailResponse.AssignmentDto.builder()
                    .assignmentId(currentAssignment.getAssignmentId())
                    .technicianId(currentAssignment.getTechnician().getUserId())
                    .technicianName(currentAssignment.getTechnician().getFullName())
                    .assignedByName(currentAssignment.getAssignedBy().getFullName())
                    .assignedAt(currentAssignment.getAssignedAt())
                    .build();
        }

        
        List<MaintenanceChecklist> checklists = checklistRepository.findByEquipmentType_TypeId(
                wo.getEquipment().getType().getTypeId());
        List<InspectionResult> results = inspectionResultRepository.findByWorkOrder_WoId(woId);
        Map<Long, InspectionResult> resultMap = new HashMap<>();
        for (InspectionResult r : results) {
            resultMap.put(r.getChecklist().getChecklistId(), r);
        }

        List<WorkOrderDetailResponse.InspectionResultDetailDto> inspectionDtos = new ArrayList<>();
        for (MaintenanceChecklist item : checklists) {
            InspectionResult res = resultMap.get(item.getChecklistId());
            inspectionDtos.add(WorkOrderDetailResponse.InspectionResultDetailDto.builder()
                    .checklistId(item.getChecklistId())
                    .item(item.getItem())
                    .isRequired(item.getIsRequired())
                    .resultId(res != null ? res.getResultId() : null)
                    .done(res != null ? res.getDone() : false)
                    .condition(res != null ? res.getCondition() : null)
                    .defect(res != null ? res.getDefect() : null)
                    .workDone(res != null ? res.getWorkDone() : null)
                    .enteredByName(res != null && res.getEnteredBy() != null ? res.getEnteredBy().getFullName() : null)
                    .enteredAt(res != null ? res.getEnteredAt() : null)
                    .build());
        }

        
        List<WorkOrderDetailResponse.AcceptanceHistoryDto> acceptanceDtos = acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(woId)
                .stream()
                .map(a -> WorkOrderDetailResponse.AcceptanceHistoryDto.builder()
                        .acceptanceId(a.getAcceptanceId())
                        .decision(a.getDecision())
                        .note(a.getNote())
                        .acceptedByName(a.getAcceptedBy() != null ? a.getAcceptedBy().getFullName() : null)
                        .acceptedAt(a.getAcceptedAt())
                        .build())
                .toList();

        LocalDate nextDueDate = planRepository.findByEquipment_EquipmentId(wo.getEquipment().getEquipmentId())
                .map(MaintenancePlan::getNextDueDate)
                .orElse(null);

        return WorkOrderDetailResponse.builder()
                .woId(wo.getWoId())
                .woCode(wo.getWoCode())
                .equipment(EquipmentResponse.fromEntity(wo.getEquipment(), nextDueDate))
                .type(wo.getType())
                .period(wo.getPeriod())
                .status(wo.getStatus())
                .createdAt(wo.getCreatedAt())
                .closedAt(wo.getClosedAt())
                .incidentId(wo.getIncident() != null ? wo.getIncident().getIncidentId() : null)
                .incidentTrackingCode(wo.getIncident() != null ? wo.getIncident().getTrackingCode() : null)
                .currentAssignment(assignmentDto)
                .inspectionResults(inspectionDtos)
                .acceptances(acceptanceDtos)
                .build();
    }

    @Transactional
    public WorkOrderDetailResponse createWorkOrder(CreateWorkOrderRequest request, Long managerId) {
        Equipment equipment;
        MaintenancePlan plan = null;
        IncidentReport incident = null;
        String period = null;

        if (request.getType() == WorkOrderType.PERIODIC) {
            equipment = equipmentRepository.findByIdWithLock(request.getEquipmentId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

            if (equipment.getStatus() != EquipmentStatus.ACTIVE) {
                throw new BusinessException(HttpStatus.CONFLICT, "EQUIPMENT_NOT_ACTIVE",
                        "Chỉ có thể tạo work order định kỳ cho thiết bị đang ở trạng thái ACTIVE.");
            }

            plan = planRepository.findByEquipment_EquipmentId(equipment.getEquipmentId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND",
                            "Thiết bị chưa được cấu hình kế hoạch bảo trì. Không thể tạo WO định kỳ."));

            if (plan.getNextDueDate() == null) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "NEXT_DUE_DATE_MISSING",
                        "Kế hoạch bảo trì chưa có ngày đến hạn tiếp theo.");
            }

            
            period = plan.getNextDueDate().toString();

            
            if (workOrderRepository.existsByEquipment_EquipmentIdAndTypeAndStatusNot(
                    equipment.getEquipmentId(), WorkOrderType.PERIODIC, WorkOrderStatus.CLOSED)) {
                throw new BusinessException(HttpStatus.CONFLICT, "WO_PERIOD_DUPLICATE",
                        "Thiết bị đang có work order định kỳ chưa hoàn tất (chưa CLOSED).");
            }
        } else {
            
            if (request.getIncidentId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INCIDENT_ID_REQUIRED",
                        "Work order sửa chữa bắt buộc phải gắn với một sự cố (incidentId).");
            }

            incident = incidentRepository.findByIdWithLock(request.getIncidentId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại."));

            
            if (incident.getStatus() != IncidentStatus.CLASSIFIED) {
                throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                        String.format("Chỉ sự cố đã được phân loại (CLASSIFIED) mới có thể tạo work order sửa chữa (hiện tại: %s).", incident.getStatus()));
            }

            equipment = incident.getEquipment();
            period = null;
        }

        String woCode = generateUniqueWoCode();

        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woCode(woCode)
                .equipment(equipment)
                .plan(plan)
                .incident(incident)
                .type(request.getType())
                .period(period)
                .status(WorkOrderStatus.OPEN)
                .build();

        MaintenanceWorkOrder saved = workOrderRepository.save(wo);

        historyService.record(equipment, saved, "WO_CREATE", null, WorkOrderStatus.OPEN.name(), null, "Tạo mới work order");

        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("woId", saved.getWoId());
        auditDetails.put("woCode", saved.getWoCode());
        auditDetails.put("equipmentCode", equipment.getCode());
        auditDetails.put("status", WorkOrderStatus.OPEN.name());
        if (period != null) auditDetails.put("period", period);

        auditService.record(
                "WO_CREATE",
                "WORK_ORDER",
                saved.getWoId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                auditDetails
        );

        return getWorkOrderById(saved.getWoId(), managerId, RoleName.MANAGER.name());
    }

    @Transactional
    public WorkOrderDetailResponse assignWorkOrder(Long woId, AssignWorkOrderRequest request, Long managerId) {
        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanAssign(wo.getStatus());
        authorizationPolicy.assertCanAssignTechnician(request.getTechnicianId());

        AppUser technician = userRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Technician không tồn tại."));
        AppUser manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Manager không tồn tại."));

        
        Optional<WorkOrderAssignment> currentOpt = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(woId);
        if (currentOpt.isPresent()) {
            WorkOrderAssignment old = currentOpt.get();
            old.setIsCurrent(false);
            old.setUnassignedAt(Instant.now());
            assignmentRepository.saveAndFlush(old);
        }

        
        WorkOrderAssignment newAssign = WorkOrderAssignment.builder()
                .workOrder(wo)
                .technician(technician)
                .assignedBy(manager)
                .assignedAt(Instant.now())
                .isCurrent(true)
                .build();
        assignmentRepository.save(newAssign);

        WorkOrderStatus oldStatus = wo.getStatus();
        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            wo.setStatus(WorkOrderStatus.ASSIGNED);
            workOrderRepository.save(wo);
        }

        
        if (wo.getType() == WorkOrderType.REPAIR && wo.getIncident() != null) {
            IncidentReport inc = incidentRepository.findByIdWithLock(wo.getIncident().getIncidentId()).orElse(null);
            if (inc != null && inc.getStatus() == IncidentStatus.CLASSIFIED) {
                inc.setStatus(IncidentStatus.ASSIGNED);
                incidentRepository.save(inc);
                historyService.record(wo.getEquipment(), wo, "INCIDENT_ASSIGN",
                        IncidentStatus.CLASSIFIED.name(), IncidentStatus.ASSIGNED.name(), null,
                        "Phân công kỹ thuật viên cho sự cố: " + technician.getFullName());
                auditService.record(
                        "INCIDENT_ASSIGN",
                        "INCIDENT",
                        inc.getIncidentId().toString(),
                        AuditOutcome.SUCCESS,
                        "OK",
                        Map.of("oldStatus", IncidentStatus.CLASSIFIED.name()),
                        Map.of("newStatus", IncidentStatus.ASSIGNED.name(), "technicianId", technician.getUserId())
                );
            }
        }

        historyService.record(wo.getEquipment(), wo, "WO_ASSIGN", oldStatus.name(), wo.getStatus().name(), null,
                "Phân công cho kỹ thuật viên: " + technician.getFullName());

        auditService.record(
                "WO_ASSIGN",
                "WORK_ORDER",
                wo.getWoId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("status", oldStatus.name()),
                Map.of("status", wo.getStatus().name(), "technicianId", technician.getUserId())
        );

        return getWorkOrderById(wo.getWoId(), managerId, RoleName.MANAGER.name());
    }

    @Transactional
    public WorkOrderDetailResponse startWorkOrder(Long woId, Long technicianId) {
        authorizationPolicy.assertTechnicianAssigned(woId, technicianId);

        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanStart(wo.getStatus());

        Equipment equipment = equipmentRepository.findByIdWithLock(wo.getEquipment().getEquipmentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

       
        WorkOrderStatus oldWoStatus = wo.getStatus();
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrderRepository.save(wo);

        
        EquipmentStatus oldEqStatus = equipment.getStatus();
        EquipmentStatus newEqStatus = equipmentStateMachine.determineStatusOnWoStart(oldEqStatus, wo.getType());
        if (oldEqStatus != newEqStatus) {
            equipment.setStatus(newEqStatus);
            equipmentRepository.save(equipment);
            historyService.record(equipment, wo, "EQUIPMENT_STATUS_CHANGE", oldEqStatus.name(), newEqStatus.name(), null,
                    "Chuyển trạng thái sang " + newEqStatus + " khi bắt đầu thực hiện work order");
        }

        
        if (wo.getType() == WorkOrderType.REPAIR && wo.getIncident() != null) {
            IncidentReport inc = incidentRepository.findByIdWithLock(wo.getIncident().getIncidentId()).orElse(null);
            if (inc != null && inc.getStatus() == IncidentStatus.ASSIGNED) {
                inc.setStatus(IncidentStatus.IN_REPAIR);
                incidentRepository.save(inc);
                historyService.record(equipment, wo, "INCIDENT_PROGRESS",
                        IncidentStatus.ASSIGNED.name(), IncidentStatus.IN_REPAIR.name(), null,
                        "Bắt đầu sửa chữa sự cố");
                auditService.record(
                        "INCIDENT_PROGRESS",
                        "INCIDENT",
                        inc.getIncidentId().toString(),
                        AuditOutcome.SUCCESS,
                        "OK",
                        Map.of("oldStatus", IncidentStatus.ASSIGNED.name()),
                        Map.of("newStatus", IncidentStatus.IN_REPAIR.name())
                );
            }
        }

        historyService.record(equipment, wo, "WO_START", oldWoStatus.name(), WorkOrderStatus.IN_PROGRESS.name(), null,
                "Bắt đầu thực hiện work order");

        auditService.record(
                "WO_START",
                "WORK_ORDER",
                wo.getWoId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("status", oldWoStatus.name()),
                Map.of("status", WorkOrderStatus.IN_PROGRESS.name())
        );

        return getWorkOrderById(wo.getWoId(), technicianId, RoleName.TECHNICIAN.name());
    }

    @Transactional
    public WorkOrderDetailResponse updateInspectionResults(Long woId, UpdateInspectionResultsRequest request, Long technicianId) {
        authorizationPolicy.assertTechnicianAssigned(woId, technicianId);

        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanUpdateResults(wo.getStatus());

        AppUser tech = userRepository.findById(technicianId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Technician không tồn tại."));

        for (InspectionResultItemDto itemDto : request.getItems()) {
            MaintenanceChecklist checklist = checklistRepository.findById(itemDto.getChecklistId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_NOT_FOUND",
                            "Hạng mục checklist ID " + itemDto.getChecklistId() + " không tồn tại."));

            InspectionResult result = inspectionResultRepository
                    .findByWorkOrder_WoIdAndChecklist_ChecklistId(woId, itemDto.getChecklistId())
                    .orElse(null);

            if (result == null) {
                result = InspectionResult.builder()
                        .workOrder(wo)
                        .checklist(checklist)
                        .build();
            }

            result.setDone(Boolean.TRUE.equals(itemDto.getDone()));
            result.setCondition(itemDto.getCondition());
            result.setDefect(itemDto.getDefect());
            result.setWorkDone(itemDto.getWorkDone());
            result.setEnteredBy(tech);
            result.setEnteredAt(Instant.now());

            inspectionResultRepository.save(result);
        }

        auditService.record(
                "WO_RESULT_UPDATE",
                "WORK_ORDER",
                woId.toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                Map.of("itemCount", request.getItems().size())
        );

        return getWorkOrderById(woId, technicianId, RoleName.TECHNICIAN.name());
    }

    @Transactional
    public WorkOrderDetailResponse submitWorkOrder(Long woId, Long technicianId) {
        authorizationPolicy.assertTechnicianAssigned(woId, technicianId);

        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanSubmit(wo.getStatus());

        
        List<MaintenanceChecklist> requiredChecklists = checklistRepository
                .findByEquipmentType_TypeId(wo.getEquipment().getType().getTypeId())
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsRequired()))
                .toList();

        List<String> missingItems = new ArrayList<>();
        for (MaintenanceChecklist req : requiredChecklists) {
            Optional<InspectionResult> resOpt = inspectionResultRepository
                    .findByWorkOrder_WoIdAndChecklist_ChecklistId(woId, req.getChecklistId());
            if (resOpt.isEmpty() || !Boolean.TRUE.equals(resOpt.get().getDone())) {
                missingItems.add(req.getItem());
            }
        }

        if (!missingItems.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CHECKLIST_INCOMPLETE",
                    "Chưa hoàn thành tất cả các hạng mục checklist bắt buộc.",
                    Map.of("missingItems", missingItems));
        }

        WorkOrderStatus oldStatus = wo.getStatus();
        wo.setStatus(WorkOrderStatus.SUBMITTED);
        workOrderRepository.save(wo);

        historyService.record(wo.getEquipment(), wo, "WO_SUBMIT", oldStatus.name(), WorkOrderStatus.SUBMITTED.name(), null,
                "Nộp nghiệm thu work order");

        auditService.record(
                "WO_SUBMIT",
                "WORK_ORDER",
                wo.getWoId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("status", oldStatus.name()),
                Map.of("status", WorkOrderStatus.SUBMITTED.name())
        );

        return getWorkOrderById(wo.getWoId(), technicianId, RoleName.TECHNICIAN.name());
    }

    @Transactional
    public WorkOrderDetailResponse acceptWorkOrder(Long woId, AcceptWorkOrderRequest request, Long managerId) {
        if (request.getDecision() == AcceptanceDecision.FAIL) {
            if (request.getNote() == null || request.getNote().trim().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "NOTE_REQUIRED",
                        "Ghi chú bắt buộc khi nghiệm thu Không Đạt (FAIL).");
            }
        }

        
        authorizationPolicy.assertNotSelfAcceptance(woId, managerId);

        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanAccept(wo.getStatus());

        AppUser manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Manager không tồn tại."));

        Equipment equipment = equipmentRepository.findByIdWithLock(wo.getEquipment().getEquipmentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        if (request.getDecision() == AcceptanceDecision.PASS) {
            WorkOrderStatus oldWoStatus = wo.getStatus();
            wo.setStatus(WorkOrderStatus.CLOSED);
            wo.setClosedAt(Instant.now());
            workOrderRepository.save(wo);

            if (wo.getType() == WorkOrderType.PERIODIC) {
                MaintenancePlan plan = planRepository.findByEquipment_EquipmentId(equipment.getEquipmentId())
                        .orElse(null);
                if (plan != null) {
                    LocalDate today = LocalDate.now();
                    plan.setLastCompletedDate(today);
                    plan.setNextDueDate(today.plusDays(plan.getCycleDays()));
                    planRepository.save(plan);
                }
            }

            EquipmentStatus oldEqStatus = equipment.getStatus();
            equipment.setStatus(EquipmentStatus.ACTIVE);
            equipmentRepository.save(equipment);

            if (oldEqStatus != EquipmentStatus.ACTIVE) {
                historyService.record(equipment, wo, "EQUIPMENT_STATUS_CHANGE",
                        oldEqStatus.name(), EquipmentStatus.ACTIVE.name(), null,
                        "Thiết bị hoạt động trở lại sau khi nghiệm thu work order đạt yêu cầu");
            }

            WorkOrderAcceptance acceptance = WorkOrderAcceptance.builder()
                    .workOrder(wo)
                    .decision(AcceptanceDecision.PASS)
                    .note(request.getNote())
                    .acceptedBy(manager)
                    .acceptedAt(Instant.now())
                    .build();
            acceptanceRepository.save(acceptance);

            historyService.record(equipment, wo, "WO_ACCEPT",
                    oldWoStatus.name(), WorkOrderStatus.CLOSED.name(), null,
                    "Nghiệm thu ĐẠT và đóng work order: " + (request.getNote() != null ? request.getNote() : ""));

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("decision", "PASS");
            auditDetails.put("status", WorkOrderStatus.CLOSED.name());
            if (request.getNote() != null) auditDetails.put("note", request.getNote());

            auditService.record(
                    "WO_ACCEPT",
                    "WORK_ORDER",
                    wo.getWoId().toString(),
                    AuditOutcome.SUCCESS,
                    "OK",
                    Map.of("status", oldWoStatus.name()),
                    auditDetails
            );
        } else {
            WorkOrderStatus oldWoStatus = wo.getStatus();
            wo.setStatus(WorkOrderStatus.IN_PROGRESS);
            workOrderRepository.save(wo);

            WorkOrderAcceptance acceptance = WorkOrderAcceptance.builder()
                    .workOrder(wo)
                    .decision(AcceptanceDecision.FAIL)
                    .note(request.getNote())
                    .acceptedBy(manager)
                    .acceptedAt(Instant.now())
                    .build();
            acceptanceRepository.save(acceptance);

            historyService.record(equipment, wo, "WO_ACCEPT_FAIL",
                    oldWoStatus.name(), WorkOrderStatus.IN_PROGRESS.name(), null,
                    "Nghiệm thu KHÔNG ĐẠT: " + request.getNote());

            auditService.record(
                    "WO_ACCEPT",
                    "WORK_ORDER",
                    wo.getWoId().toString(),
                    AuditOutcome.SUCCESS,
                    "OK",
                    Map.of("status", oldWoStatus.name()),
                    Map.of("decision", "FAIL", "status", WorkOrderStatus.IN_PROGRESS.name(), "note", request.getNote())
            );
        }

        return getWorkOrderById(wo.getWoId(), managerId, RoleName.MANAGER.name());
    }

    @Transactional
    public WorkOrderAdjustmentResponse createAdjustment(Long woId, CreateAdjustmentRequest request, Long managerId) {
        MaintenanceWorkOrder wo = workOrderRepository.findByIdWithLock(woId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WO_NOT_FOUND", "Work order không tồn tại."));

        workOrderStateMachine.assertCanAdjust(wo.getStatus());

        AppUser manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Manager không tồn tại."));

        WorkOrderAdjustment adjustment = WorkOrderAdjustment.builder()
                .workOrder(wo)
                .reason(request.getReason())
                .changeDetail(request.getChangeDetail())
                .createdBy(manager)
                .createdAt(Instant.now())
                .build();
        WorkOrderAdjustment saved = adjustmentRepository.save(adjustment);

        historyService.record(wo.getEquipment(), wo, "WO_ADJUSTMENT",
                wo.getStatus().name(), wo.getStatus().name(), null,
                "Bản ghi điều chỉnh sau đóng: " + request.getReason());

        auditService.record(
                "WO_ADJUSTMENT",
                "WORK_ORDER",
                wo.getWoId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                Map.of("adjustmentId", saved.getAdjustmentId(), "reason", saved.getReason())
        );

        return WorkOrderAdjustmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderAdjustmentResponse> getAdjustments(Long woId, Long currentUserId, String currentRole) {
        authorizationPolicy.assertCanReadWorkOrder(woId, currentUserId, currentRole);
        return adjustmentRepository.findByWorkOrder_WoIdOrderByCreatedAtDesc(woId)
                .stream()
                .map(WorkOrderAdjustmentResponse::fromEntity)
                .toList();
    }

    private String generateUniqueWoCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int seq = secureRandom.nextInt(9000) + 1000;
            String code = "WO-" + datePart + "-" + seq;
            if (!workOrderRepository.existsByWoCode(code)) {
                return code;
            }
        }
        return "WO-" + datePart + "-" + System.currentTimeMillis() % 10000;
    }
}

