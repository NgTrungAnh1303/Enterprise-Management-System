package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.IncidentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class IncidentStateMachine {

    private static final Map<IncidentStatus, Set<IncidentStatus>> VALID_TRANSITIONS = Map.of(
            IncidentStatus.REPORTED, Set.of(IncidentStatus.CLASSIFIED),
            IncidentStatus.CLASSIFIED, Set.of(IncidentStatus.ASSIGNED),
            IncidentStatus.ASSIGNED, Set.of(IncidentStatus.IN_REPAIR),
            IncidentStatus.IN_REPAIR, Set.of(IncidentStatus.RESOLVED),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED),
            IncidentStatus.CLOSED, Set.of()
    );

    public boolean isValidTransition(IncidentStatus current, IncidentStatus next) {
        if (current == null) {
            return next == IncidentStatus.REPORTED;
        }
        if (next == null || current == next) {
            return true;
        }
        Set<IncidentStatus> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    public void validateTransition(IncidentStatus current, IncidentStatus next) {
        if (current == IncidentStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_CLOSED_IMMUTABLE",
                    "Sự cố đã đóng (CLOSED), không thể thay đổi trạng thái.");
        }
        if (!isValidTransition(current, next)) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Không thể chuyển trạng thái sự cố từ %s sang %s.", current, next));
        }
    }

    public void assertCanClassify(IncidentStatus current) {
        if (current == IncidentStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_CLOSED_IMMUTABLE", "Sự cố đã đóng.");
        }
        if (current != IncidentStatus.REPORTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ sự cố ở trạng thái REPORTED mới có thể phân loại (hiện tại: %s).", current));
        }
    }

    public void assertCanCreateRepairWo(IncidentStatus current) {
        if (current != IncidentStatus.CLASSIFIED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ sự cố đã được phân loại (CLASSIFIED) mới có thể tạo work order sửa chữa (hiện tại: %s).", current));
        }
    }

    public void assertCanProgress(IncidentStatus current) {
        if (current == IncidentStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_CLOSED_IMMUTABLE", "Sự cố đã đóng.");
        }
        if (current != IncidentStatus.ASSIGNED && current != IncidentStatus.IN_REPAIR) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Không thể cập nhật tiến độ cho sự cố ở trạng thái %s.", current));
        }
    }

    public void assertCanClose(IncidentStatus current) {
        if (current == IncidentStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_CLOSED_IMMUTABLE", "Sự cố đã đóng.");
        }
        if (current != IncidentStatus.RESOLVED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ sự cố ở trạng thái RESOLVED mới có thể đóng (hiện tại: %s).", current));
        }
    }
}

