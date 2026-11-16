package com.fpt.ems.be.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AuditService auditService;

    @ExceptionHandler(AccessDeniedBusinessException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedBusinessException(
            AccessDeniedBusinessException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.warn("Access denied on {}: code={}, message={}", request.getRequestURI(), ex.getReasonCode(), ex.getMessage());

        String targetType = ex.getTargetType() != null && !"ENDPOINT".equals(ex.getTargetType()) ? ex.getTargetType() : "ENDPOINT";
        String targetId = ex.getTargetId() != null ? ex.getTargetId() : request.getRequestURI();

        auditService.recordDenied(
                "ACCESS_DENIED",
                targetType,
                targetId,
                AuditOutcome.DENIED,
                ex.getReasonCode(),
                null,
                Map.of("path", request.getRequestURI(), "reason", ex.getMessage())
        );

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .code(ex.getReasonCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        auditService.recordDenied(
                "ACCESS_DENIED",
                "ENDPOINT",
                request.getRequestURI(),
                AuditOutcome.DENIED,
                "FORBIDDEN",
                null,
                Map.of("path", request.getRequestURI())
        );

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .code("ACCESS_DENIED")
                .message("Bạn không có quyền thực hiện thao tác này.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .code("UNAUTHORIZED")
                .message("Yêu cầu xác thực hợp lệ để truy cập tài nguyên.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.warn("Business exception on {}: code={}, message={}", request.getRequestURI(), ex.getCode(), ex.getMessage());

        if (ex.getStatus() == HttpStatus.CONFLICT || ex.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("path", request.getRequestURI());
            detailMap.put("reason", ex.getMessage());
            if (ex.getDetails() != null) {
                detailMap.put("details", ex.getDetails());
            }
            auditService.recordDenied(
                    "STATE_VIOLATION",
                    "ENDPOINT",
                    request.getRequestURI(),
                    AuditOutcome.DENIED,
                    ex.getCode(),
                    null,
                    detailMap
            );
        } else if (ex.getStatus().is4xxClientError() && ex.getStatus() != HttpStatus.NOT_FOUND) {
            auditService.recordDenied(
                    "BUSINESS_REJECTED",
                    "ENDPOINT",
                    request.getRequestURI(),
                    AuditOutcome.DENIED,
                    ex.getCode(),
                    null,
                    Map.of("path", request.getRequestURI(), "reason", ex.getMessage())
            );
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(ex.getStatus().value())
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code("VALIDATION_ERROR")
                .message("Dữ liệu gửi lên không hợp lệ.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        String message = "Định dạng dữ liệu không hợp lệ.";
        String code = "INVALID_FORMAT";

        Throwable cause = ex;
        while (cause != null) {
            String className = cause.getClass().getSimpleName();
            if ("UnrecognizedPropertyException".equals(className)) {
                code = "UNKNOWN_PROPERTY";
                message = "Trường không hợp lệ hoặc không được phép: " + cause.getMessage();
                try {
                    java.lang.reflect.Method getPropertyMethod = cause.getClass().getMethod("getPropertyName");
                    Object prop = getPropertyMethod.invoke(cause);
                    if (prop != null) {
                        message = "Trường không hợp lệ hoặc không được phép: " + prop;
                    }
                } catch (Exception ignored) {}
                break;
            }
            cause = cause.getCause();
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .code("METHOD_NOT_ALLOWED")
                .message("Phương thức " + ex.getMethod() + " không được hỗ trợ trên tài nguyên này.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .code("RESOURCE_NOT_FOUND")
                .message("Không tìm thấy tài nguyên: " + request.getRequestURI())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.warn("Database constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());

        String code = "CONFLICT";
        String message = "Dữ liệu xung đột hoặc vi phạm ràng buộc toàn vẹn.";

        String errorMsg = ex.getMessage() != null ? ex.getMessage() : "";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            errorMsg += " " + ex.getCause().getMessage();
        }

        if (errorMsg.contains("uq_periodic_wo_open")) {
            code = "WO_PERIOD_DUPLICATE";
            message = "Thiết bị đã có work order định kỳ mở trong kỳ này.";
        } else if (errorMsg.contains("equipment_code_key") || errorMsg.contains("uq_equipment_code")) {
            code = "EQUIPMENT_CODE_EXISTS";
            message = "Mã thiết bị đã tồn tại trong hệ thống.";
        } else if (errorMsg.contains("app_user_username_key")) {
            code = "USERNAME_EXISTS";
            message = "Tên đăng nhập đã tồn tại trong hệ thống.";
        } else if (errorMsg.contains("trg_block_closed_wo_modification")) {
            code = "WO_CLOSED_IMMUTABLE";
            message = "Work order đã đóng, không thể sửa đổi.";
        }

        auditService.recordDenied(
                "STATE_VIOLATION",
                "ENDPOINT",
                request.getRequestURI(),
                AuditOutcome.DENIED,
                code,
                null,
                Map.of("path", request.getRequestURI(), "reason", message)
        );

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.error("Internal Server Error on {} [requestId={}]: ", request.getRequestURI(), requestId, ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_SERVER_ERROR")
                .message("Đã xảy ra lỗi hệ thống, vui lòng thử lại sau hoặc liên hệ quản trị viên.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
