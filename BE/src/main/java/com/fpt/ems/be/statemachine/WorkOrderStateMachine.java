package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class WorkOrderStateMachine {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> VALID_TRANSITIONS = Map.of(
            WorkOrderStatus.OPEN, Set.of(WorkOrderStatus.ASSIGNED),
            WorkOrderStatus.ASSIGNED, Set.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.SUBMITTED),
            WorkOrderStatus.SUBMITTED, Set.of(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS),
            WorkOrderStatus.CLOSED, Set.of()
    );

    public boolean isValidTransition(WorkOrderStatus current, WorkOrderStatus next) {
        if (current == null) {
            return next == WorkOrderStatus.OPEN;
        }
        if (next == null) {
            return false;
        }
        Set<WorkOrderStatus> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    public void validateTransition(WorkOrderStatus current, WorkOrderStatus next) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng (CLOSED), không thể thay đổi trạng thái hoặc chỉnh sửa trực tiếp.");
        }
        if (!isValidTransition(current, next)) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Không thể chuyển trạng thái work order từ %s sang %s.", current, next));
        }
    }

    public void assertCanAssign(WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng, không thể phân công lại.");
        }
        if (current != WorkOrderStatus.OPEN && current != WorkOrderStatus.ASSIGNED && current != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Work order ở trạng thái %s không thể phân công.", current));
        }
    }

    public void assertCanStart(WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng, không thể bắt đầu.");
        }
        if (current != WorkOrderStatus.ASSIGNED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ work order ở trạng thái ASSIGNED mới có thể bắt đầu (hiện tại: %s).", current));
        }
    }

    public void assertCanUpdateResults(WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng, không thể cập nhật kết quả kiểm tra.");
        }
        if (current != WorkOrderStatus.IN_PROGRESS && current != WorkOrderStatus.ASSIGNED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ work order đang thực hiện (IN_PROGRESS/ASSIGNED) mới có thể cập nhật kết quả (hiện tại: %s).", current));
        }
    }

    public void assertCanSubmit(WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng, không thể nộp nghiệm thu.");
        }
        if (current != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ work order ở trạng thái IN_PROGRESS mới có thể nộp nghiệm thu (hiện tại: %s).", current));
        }
    }

    public void assertCanAccept(WorkOrderStatus current) {
        if (current == WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "WO_CLOSED_IMMUTABLE",
                    "Work order đã đóng, không thể nghiệm thu lại.");
        }
        if (current != WorkOrderStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_SUBMITTED",
                    String.format("Chỉ work order ở trạng thái SUBMITTED mới có thể nghiệm thu (hiện tại: %s).", current));
        }
    }

    public void assertCanAdjust(WorkOrderStatus current) {
        if (current != WorkOrderStatus.CLOSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    String.format("Chỉ work order ở trạng thái CLOSED mới có thể tạo bản ghi điều chỉnh (hiện tại: %s).", current));
        }
    }
}

