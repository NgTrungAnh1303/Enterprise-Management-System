package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.history.MaintenanceHistoryResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.MaintenanceHistory;
import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.repository.MaintenanceHistoryRepository;
import com.fpt.ems.be.security.AuthorizationPolicy;
import com.fpt.ems.be.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceHistoryService {

    private final MaintenanceHistoryRepository historyRepository;
    private final EquipmentRepository equipmentRepository;
    private final AppUserRepository userRepository;
    private final AuthorizationPolicy authorizationPolicy;

    @Transactional(propagation = Propagation.REQUIRED)
    public MaintenanceHistory record(Equipment equipment, MaintenanceWorkOrder workOrder,
                                     String action, String oldStatus, String newStatus,
                                     AppUser explicitUser, String note) {
        AppUser performer = explicitUser;
        if (performer == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                performer = userRepository.findById(principal.getUserId()).orElse(null);
            }
        }

        MaintenanceHistory history = MaintenanceHistory.builder()
                .equipment(equipment)
                .workOrder(workOrder)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .performedBy(performer)
                .performedAt(Instant.now())
                .note(note)
                .build();

        return historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceHistoryResponse> getEquipmentHistory(Long equipmentId, int page, int size,
                                                                       Long currentUserId, String currentRole) {
        authorizationPolicy.assertCanReadEquipmentHistory(equipmentId, currentUserId, currentRole);

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Thiết bị không tồn tại.");
        }

        if (size > 100) size = 100;
        if (size < 1) size = 20;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size);
        Page<MaintenanceHistory> pageResult = historyRepository.findByEquipment_EquipmentIdOrderByPerformedAtDesc(equipmentId, pageable);

        List<MaintenanceHistoryResponse> items = pageResult.getContent().stream()
                .map(MaintenanceHistoryResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }
}

