package com.fpt.ems.be.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccessDeniedBusinessException extends BusinessException {
    private final String reasonCode;
    private final String targetType;
    private final String targetId;

    public AccessDeniedBusinessException(String reasonCode, String message) {
        this(reasonCode, message, "ENDPOINT", null);
    }

    public AccessDeniedBusinessException(String reasonCode, String message, String targetType, String targetId) {
        super(HttpStatus.FORBIDDEN, reasonCode, message);
        this.reasonCode = reasonCode;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
