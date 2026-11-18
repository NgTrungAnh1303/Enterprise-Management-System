package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.master.EquipmentTypeRequest;
import com.fpt.ems.be.dto.master.EquipmentTypeResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.EquipmentType;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.EquipmentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentTypeService {

    private final EquipmentTypeRepository typeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EquipmentTypeResponse> getAllTypes() {
        return typeRepository.findAll().stream()
                .map(EquipmentTypeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentTypeResponse getTypeById(Long id) {
        EquipmentType type = typeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại."));
        return EquipmentTypeResponse.fromEntity(type);
    }

    @Transactional
    public EquipmentTypeResponse createType(EquipmentTypeRequest request) {
        String name = request.getName().trim();
        if (typeRepository.existsByName(name)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TYPE_NAME_EXISTS", "Tên loại thiết bị đã tồn tại.");
        }

        EquipmentType type = EquipmentType.builder()
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        EquipmentType saved = typeRepository.save(type);

        auditService.record(
                "EQUIPMENT_TYPE_CREATE",
                "EQUIPMENT_TYPE",
                saved.getTypeId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                Map.of("name", saved.getName())
        );

        return EquipmentTypeResponse.fromEntity(saved);
    }

    @Transactional
    public EquipmentTypeResponse updateType(Long id, EquipmentTypeRequest request) {
        EquipmentType type = typeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại."));

        String name = request.getName().trim();
        if (typeRepository.existsByNameAndTypeIdNot(name, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TYPE_NAME_EXISTS", "Tên loại thiết bị đã tồn tại.");
        }

        Map<String, Object> oldVal = Map.of("name", type.getName());

        type.setName(name);
        if (request.getDescription() != null) {
            type.setDescription(request.getDescription().trim());
        }

        EquipmentType updated = typeRepository.save(type);

        auditService.record(
                "EQUIPMENT_TYPE_UPDATE",
                "EQUIPMENT_TYPE",
                updated.getTypeId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                oldVal,
                Map.of("name", updated.getName())
        );

        return EquipmentTypeResponse.fromEntity(updated);
    }
}

