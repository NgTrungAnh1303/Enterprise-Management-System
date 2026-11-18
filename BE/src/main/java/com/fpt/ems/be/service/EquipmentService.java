package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.master.EquipmentCreateRequest;
import com.fpt.ems.be.dto.master.EquipmentResponse;
import com.fpt.ems.be.dto.master.EquipmentUpdateRequest;
import com.fpt.ems.be.dto.statusrequest.StatusChangeRequestResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.EquipmentType;
import com.fpt.ems.be.model.Location;
import com.fpt.ems.be.model.MaintenancePlan;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.StatusRequestStatus;
import com.fpt.ems.be.repository.*;
import com.fpt.ems.be.security.AuthorizationPolicy;
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

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository typeRepository;
    private final LocationRepository locationRepository;
    private final MaintenancePlanRepository planRepository;
    private final StatusChangeRequestRepository statusChangeRequestRepository;
    private final MaintenanceHistoryService historyService;
    private final AuditService auditService;
    private final AuthorizationPolicy authorizationPolicy;

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> getEquipments(String q, Long typeId, Long locationId,
                                                         EquipmentStatus status, int page, int size,
                                                         Long currentUserId, String currentRole) {
        if (size > 100) size = 100;
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "code"));

        Specification<Equipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search query (code, brand, model, serial)
            if (q != null && !q.isBlank()) {
                String searchPattern = "%" + q.trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("code")), searchPattern);
                Predicate brandPred = cb.like(cb.lower(root.get("brand")), searchPattern);
                Predicate modelPred = cb.like(cb.lower(root.get("model")), searchPattern);
                Predicate serialPred = cb.like(cb.lower(root.get("serial")), searchPattern);
                predicates.add(cb.or(codePred, brandPred, modelPred, serialPred));
            }

            if (typeId != null) {
                predicates.add(cb.equal(root.get("type").get("typeId"), typeId));
            }

            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("locationId"), locationId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Role filtering for Technician: only see equipment involved in assigned work orders
            if (RoleName.TECHNICIAN.name().equals(currentRole)) {
                var subquery = query.subquery(Long.class);
                var assignRoot = subquery.from(com.fpt.ems.be.model.WorkOrderAssignment.class);
                subquery.select(assignRoot.get("workOrder").get("equipment").get("equipmentId"))
                        .where(cb.equal(assignRoot.get("technician").get("userId"), currentUserId));
                predicates.add(root.get("equipmentId").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Equipment> equipmentPage = equipmentRepository.findAll(spec, pageable);

        List<EquipmentResponse> items = equipmentPage.getContent().stream()
                .map(eq -> {
                    LocalDate nextDueDate = planRepository.findByEquipment_EquipmentId(eq.getEquipmentId())
                            .map(MaintenancePlan::getNextDueDate)
                            .orElse(null);
                    return EquipmentResponse.fromEntity(eq, nextDueDate);
                })
                .toList();

        return new PageResponse<>(items, equipmentPage.getNumber(), equipmentPage.getSize(), equipmentPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipmentById(Long id, Long currentUserId, String currentRole) {
        authorizationPolicy.assertCanReadEquipment(id, currentUserId, currentRole);

        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        LocalDate nextDueDate = planRepository.findByEquipment_EquipmentId(id)
                .map(MaintenancePlan::getNextDueDate)
                .orElse(null);

        EquipmentResponse response = EquipmentResponse.fromEntity(equipment, nextDueDate);

        if (currentRole != null) {
            if (RoleName.MANAGER.name().equals(currentRole)) {
                statusChangeRequestRepository.findByEquipment_EquipmentIdAndStatus(id, StatusRequestStatus.PENDING)
                        .stream()
                        .findFirst()
                        .map(StatusChangeRequestResponse::fromEntity)
                        .ifPresent(response::setPendingStatusRequest);
            } else if (RoleName.TECHNICIAN.name().equals(currentRole) && currentUserId != null) {
                statusChangeRequestRepository.findByEquipment_EquipmentIdAndStatus(id, StatusRequestStatus.PENDING)
                        .stream()
                        .filter(req -> req.getProposedBy() != null && currentUserId.equals(req.getProposedBy().getUserId()))
                        .findFirst()
                        .map(StatusChangeRequestResponse::fromEntity)
                        .ifPresent(response::setPendingStatusRequest);
            }
        }

        return response;
    }

    @Transactional
    public EquipmentResponse createEquipment(EquipmentCreateRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (equipmentRepository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EQUIPMENT_CODE_EXISTS", "Mã thiết bị đã tồn tại trong hệ thống.");
        }

        EquipmentType type = typeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại."));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "LOCATION_NOT_FOUND", "Vị trí không tồn tại."));

        Equipment equipment = Equipment.builder()
                .code(code)
                .brand(request.getBrand() != null ? request.getBrand().trim() : null)
                .model(request.getModel() != null ? request.getModel().trim() : null)
                .serial(request.getSerial() != null ? request.getSerial().trim() : null)
                .type(type)
                .location(location)
                .status(EquipmentStatus.ACTIVE)
                .build();

        Equipment saved = equipmentRepository.save(equipment);

        // Record history and audit in same transaction
        historyService.record(saved, null, "EQUIPMENT_CREATE", null, EquipmentStatus.ACTIVE.name(), null, "Tạo mới thiết bị");

        auditService.record(
                "EQUIPMENT_CREATE",
                "EQUIPMENT",
                saved.getEquipmentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                Map.of("equipmentCode", saved.getCode(), "status", EquipmentStatus.ACTIVE.name())
        );

        return EquipmentResponse.fromEntity(saved, null);
    }

    @Transactional
    public EquipmentResponse updateEquipment(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = equipmentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại."));

        EquipmentType type = typeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại."));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "LOCATION_NOT_FOUND", "Vị trí không tồn tại."));

        Map<String, Object> oldVal = Map.of(
                "brand", equipment.getBrand() != null ? equipment.getBrand() : "",
                "model", equipment.getModel() != null ? equipment.getModel() : ""
        );

        equipment.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
        equipment.setModel(request.getModel() != null ? request.getModel().trim() : null);
        equipment.setSerial(request.getSerial() != null ? request.getSerial().trim() : null);
        equipment.setType(type);
        equipment.setLocation(location);
        // Note: code and status remain unchanged per specification

        Equipment updated = equipmentRepository.save(equipment);

        historyService.record(updated, null, "EQUIPMENT_UPDATE", updated.getStatus().name(), updated.getStatus().name(), null, "Cập nhật thông tin thiết bị");

        auditService.record(
                "EQUIPMENT_UPDATE",
                "EQUIPMENT",
                updated.getEquipmentId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                oldVal,
                Map.of("equipmentCode", updated.getCode())
        );

        LocalDate nextDueDate = planRepository.findByEquipment_EquipmentId(id)
                .map(MaintenancePlan::getNextDueDate)
                .orElse(null);

        return EquipmentResponse.fromEntity(updated, nextDueDate);
    }
}
