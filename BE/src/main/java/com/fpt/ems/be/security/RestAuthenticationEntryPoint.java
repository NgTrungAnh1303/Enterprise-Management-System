package com.fpt.ems.be.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.exception.ErrorResponse;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String requestId = MDC.get("requestId");
        String reason = (String) request.getAttribute("JWT_ERROR_REASON");
        String code = "UNAUTHORIZED";
        String message = "Yêu cầu xác thực hợp lệ để truy cập tài nguyên.";

        if (reason != null) {
            code = "TOKEN_REJECTED";
            message = reason;
        }

        auditService.recordDenied(
                "TOKEN_REJECTED",
                "ENDPOINT",
                request.getRequestURI(),
                AuditOutcome.DENIED,
                code,
                null,
                Map.of("path", request.getRequestURI(), "reason", message)
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

