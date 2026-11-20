package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.incident.ClassifyIncidentRequest;
import com.fpt.ems.be.dto.incident.IncidentCreateRequest;
import com.fpt.ems.be.dto.incident.IncidentProgressRequest;
import com.fpt.ems.be.dto.incident.IncidentResponse;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.IncidentReport;
import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.WorkOrderAssignment;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.IncidentSeverity;
import com.fpt.ems.be.model.enums.IncidentStatus;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.IncidentReportRepository;
import com.fpt.ems.be.repository.MaintenanceWorkOrderRepository;
import com.fpt.ems.be.repository.WorkOrderAssignmentRepository;
import com.fpt.ems.be.security.AuthorizationPolicy;
import com.fpt.ems.be.statemachine.IncidentStateMachine;
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
public class IncidentReportService {

    private final IncidentReportRepository incidentRepository;
    private final EquipmentRepository equipmentRepository;
    private final AppUserRepository userRepository;
    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository assignmentRepository;
    private final IncidentStateMachine incidentStateMachine;
    private final AuthorizationPolicy authorizationPolicy;
    private final MaintenanceHistoryService historyService;
    private final AuditService auditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IncidentResponse createIncident(IncidentCreateRequest request, Long reporterUserId) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        AppUser reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người báo cáo không tồn tại."));

        String trackingCode = generateUniqueTrackingCode();

        IncidentReport incident = IncidentReport.builder()
                .trackingCode(trackingCode)
                .equipment(equipment)
                .reporter(reporter)
                .description(request.getDescription())
                .status(IncidentStatus.REPORTED)
                .createdAt(Instant.now())
                .build();

        IncidentReport saved = incidentRepository.save(incident);

        historyService.record(equipment, null, "INCIDENT_REPORT",
                null, IncidentStatus.REPORTED.name(), null,
                "Báo cáo sự cố: " + trackingCode + " - " + request.getDescription());

        Map<String, Object> details = new HashMap<>();
        details.put("incidentId", saved.getIncidentId());
        details.put("trackingCode", trackingCode);
        details.put("equipmentCode", equipment.getCode());

        auditService.record(
                "INCIDENT_CREATE",
                "INCIDENT",
                saved.getIncidentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                details
        );

        return IncidentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> getIncidents(IncidentStatus status, IncidentSeverity severity, Long equipmentId,
                                                       int page, int size,
                                                       Long currentUserId, String currentRole) {
        if (RoleName.ADMIN.name().equals(currentRole)) {
            throw new AccessDeniedBusinessException("ACCESS_DENIED", "Admin không có quyền truy cập sự cố.");
        }

        if (size > 100) size = 100;
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<IncidentReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (equipmentId != null) {
                predicates.add(cb.equal(root.get("equipment").get("equipmentId"), equipmentId));
            }

            
            if (RoleName.EQUIPMENT_USER.name().equals(currentRole)) {
                predicates.add(cb.equal(root.get("reporter").get("userId"), currentUserId));
            } else if (RoleName.TECHNICIAN.name().equals(currentRole)) {
                var subquery = query.subquery(Long.class);
                var assignRoot = subquery.from(WorkOrderAssignment.class);
                subquery.select(assignRoot.get("workOrder").get("incident").get("incidentId"))
                        .where(cb.and(
                                cb.equal(assignRoot.get("technician").get("userId"), currentUserId),
                                cb.isNotNull(assignRoot.get("workOrder").get("incident"))
                        ));
                predicates.add(root.get("incidentId").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<IncidentReport> pageResult = incidentRepository.findAll(spec, pageable);
        List<IncidentResponse> items = pageResult.getContent().stream()
                .map(IncidentResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id, Long currentUserId, String currentRole) {
        authorizationPolicy.assertIncidentVisible(id, currentUserId, currentRole);

        IncidentReport incident = incidentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại."));

        return IncidentResponse.fromEntity(incident);
    }

    @Transactional
    public IncidentResponse classifyIncident(Long id, ClassifyIncidentRequest request, Long managerId) {
        IncidentReport incident = incidentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại."));

        incidentStateMachine.assertCanClassify(incident.getStatus());

        IncidentStatus oldStatus = incident.getStatus();
        incident.setSeverity(request.getSeverity());
        incident.setStatus(IncidentStatus.CLASSIFIED);
        IncidentReport saved = incidentRepository.save(incident);

        historyService.record(incident.getEquipment(), null, "INCIDENT_CLASSIFY",
                oldStatus.name(), IncidentStatus.CLASSIFIED.name(), null,
                "Phân loại sự cố mức độ: " + request.getSeverity());

        auditService.record(
                "INCIDENT_CLASSIFY",
                "INCIDENT",
                saved.getIncidentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("oldStatus", oldStatus.name()),
                Map.of("newStatus", IncidentStatus.CLASSIFIED.name(), "severity", request.getSeverity().name())
        );

        return IncidentResponse.fromEntity(saved);
    }

    @Transactional
    public IncidentResponse updateProgress(Long id, IncidentProgressRequest request, Long technicianId) {
        IncidentReport incident = incidentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại."));

        incidentStateMachine.assertCanProgress(incident.getStatus());

        
        boolean isAssigned = assignmentRepository.existsByWorkOrder_Incident_IncidentIdAndTechnician_UserId(id, technicianId);
        if (!isAssigned) {
            throw new AccessDeniedBusinessException("WO_NOT_ASSIGNED", "Bạn không được phân công xử lý sự cố này.");
        }

        if (request.getStatus() == IncidentStatus.RESOLVED) {
            // Check condition: repair WO must have been SUBMITTED or CLOSED
            Optional<MaintenanceWorkOrder> woOpt = workOrderRepository.findFirstByIncident_IncidentIdOrderByCreatedAtDesc(id);
            if (woOpt.isEmpty() || (woOpt.get().getStatus() != WorkOrderStatus.SUBMITTED && woOpt.get().getStatus() != WorkOrderStatus.CLOSED)) {
                throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                        "Chỉ có thể đánh dấu sự cố là RESOLVED khi work order sửa chữa đã được nộp nghiệm thu (SUBMITTED).");
            }
        }

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(request.getStatus());
        IncidentReport saved = incidentRepository.save(incident);

        historyService.record(incident.getEquipment(), null, "INCIDENT_PROGRESS",
                oldStatus.name(), request.getStatus().name(), null,
                "Cập nhật tiến độ sự cố: " + (request.getNote() != null ? request.getNote() : request.getStatus().name()));

        auditService.record(
                "INCIDENT_PROGRESS",
                "INCIDENT",
                saved.getIncidentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("status", oldStatus.name()),
                Map.of("status", request.getStatus().name(), "note", request.getNote() != null ? request.getNote() : "")
        );

        return IncidentResponse.fromEntity(saved);
    }

    @Transactional
    public IncidentResponse closeIncident(Long id, Long managerId) {
        IncidentReport incident = incidentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Sự cố không tồn tại."));

        incidentStateMachine.assertCanClose(incident.getStatus());

        
        Optional<MaintenanceWorkOrder> woOpt = workOrderRepository.findFirstByIncident_IncidentIdOrderByCreatedAtDesc(id);
        if (woOpt.isEmpty() || woOpt.get().getStatus() != WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    "Chỉ có thể đóng sự cố khi work order sửa chữa tương ứng đã được nghiệm thu ĐẠT (CLOSED).");
        }

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(Instant.now());
        IncidentReport saved = incidentRepository.save(incident);

        historyService.record(incident.getEquipment(), woOpt.orElse(null), "INCIDENT_CLOSE",
                oldStatus.name(), IncidentStatus.CLOSED.name(), null,
                "Đóng sự cố sau khi sửa chữa đã hoàn tất và nghiệm thu đạt");

        auditService.record(
                "INCIDENT_CLOSE",
                "INCIDENT",
                saved.getIncidentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("status", oldStatus.name()),
                Map.of("status", IncidentStatus.CLOSED.name())
        );

        return IncidentResponse.fromEntity(saved);
    }

    private String generateUniqueTrackingCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int seq = secureRandom.nextInt(9000) + 1000;
            String code = "INC-" + datePart + "-" + seq;
            if (!incidentRepository.existsByTrackingCode(code)) {
                return code;
            }
        }
        return "INC-" + datePart + "-" + System.currentTimeMillis() % 10000;
    }
}

