package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class EquipmentStateMachine {

    private static final Map<EquipmentStatus, Set<EquipmentStatus>> VALID_TRANSITIONS = Map.of(
            EquipmentStatus.ACTIVE, Set.of(EquipmentStatus.UNDER_MAINTENANCE, EquipmentStatus.OUT_OF_SERVICE),
            EquipmentStatus.UNDER_MAINTENANCE, Set.of(EquipmentStatus.ACTIVE, EquipmentStatus.OUT_OF_SERVICE),
            EquipmentStatus.OUT_OF_SERVICE, Set.of(EquipmentStatus.ACTIVE)
    );

    public boolean isValidTransition(EquipmentStatus current, EquipmentStatus next) {
        if (current == null) {
            return next == EquipmentStatus.ACTIVE;
        }
        if (next == null || current == next) {
            return true;
        }
        Set<EquipmentStatus> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    public void validateTransition(EquipmentStatus current, EquipmentStatus next) {
        if (!isValidTransition(current, next)) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Không thể chuyển trạng thái thiết bị từ %s sang %s.", current, next));
        }
    }

    /**
     * Determines equipment status when a work order starts.
     * Decision 7: If equipment is currently OUT_OF_SERVICE during a REPAIR work order start,
     * it MUST REMAIN OUT_OF_SERVICE (do NOT change to UNDER_MAINTENANCE).
     */
    public EquipmentStatus determineStatusOnWoStart(EquipmentStatus current, WorkOrderType type) {
        if (current == EquipmentStatus.OUT_OF_SERVICE) {
            return EquipmentStatus.OUT_OF_SERVICE;
        }
        return EquipmentStatus.UNDER_MAINTENANCE;
    }

    /**
     * Determines equipment status when a work order is accepted PASS.
     * Equipment returns to ACTIVE (even from OUT_OF_SERVICE if repair is completed).
     */
    public EquipmentStatus determineStatusOnWoPass(EquipmentStatus current, WorkOrderType type) {
        return EquipmentStatus.ACTIVE;
    }
}

