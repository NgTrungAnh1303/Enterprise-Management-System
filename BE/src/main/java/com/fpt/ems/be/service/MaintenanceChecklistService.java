package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.plan.ChecklistItemDto;
import com.fpt.ems.be.dto.plan.ChecklistUpdateRequest;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.EquipmentType;
import com.fpt.ems.be.model.MaintenanceChecklist;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.EquipmentTypeRepository;
import com.fpt.ems.be.repository.MaintenanceChecklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceChecklistService {

    private final MaintenanceChecklistRepository checklistRepository;
    private final EquipmentTypeRepository typeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ChecklistItemDto> getChecklistByTypeId(Long typeId) {
        if (!typeRepository.existsById(typeId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại.");
        }
        return checklistRepository.findByEquipmentType_TypeId(typeId).stream()
                .map(c -> ChecklistItemDto.builder()
                        .checklistId(c.getChecklistId())
                        .item(c.getItem())
                        .isRequired(c.getIsRequired())
                        .build())
                .toList();
    }

    @Transactional
    public List<ChecklistItemDto> updateChecklist(Long typeId, ChecklistUpdateRequest request) {
        EquipmentType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TYPE_NOT_FOUND", "Loại thiết bị không tồn tại."));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_EMPTY", "Danh sách checklist không được để rỗng.");
        }

        for (ChecklistItemDto itemDto : request.getItems()) {
            if (itemDto.getItem() == null || itemDto.getItem().trim().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_ITEM_EMPTY", "Hạng mục checklist không được để rỗng.");
            }
        }

        // Remove existing items
        List<MaintenanceChecklist> existingItems = checklistRepository.findByEquipmentType_TypeId(typeId);
        checklistRepository.deleteAll(existingItems);

        // Add new items
        List<MaintenanceChecklist> toSave = new ArrayList<>();
        for (ChecklistItemDto itemDto : request.getItems()) {
            toSave.add(MaintenanceChecklist.builder()
                    .equipmentType(type)
                    .item(itemDto.getItem().trim())
                    .isRequired(itemDto.getIsRequired() == null || itemDto.getIsRequired())
                    .build());
        }

        List<MaintenanceChecklist> saved = checklistRepository.saveAll(toSave);

        auditService.record(
                "CHECKLIST_UPDATE",
                "EQUIPMENT_TYPE",
                typeId.toString(),
                AuditOutcome.SUCCESS,
                "OK",
                Map.of("itemCount", existingItems.size()),
                Map.of("itemCount", saved.size())
        );

        return saved.stream()
                .map(c -> ChecklistItemDto.builder()
                        .checklistId(c.getChecklistId())
                        .item(c.getItem())
                        .isRequired(c.getIsRequired())
                        .build())
                .toList();
    }
}

