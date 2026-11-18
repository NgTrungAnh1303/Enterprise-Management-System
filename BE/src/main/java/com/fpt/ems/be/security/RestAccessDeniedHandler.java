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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String requestId = MDC.get("requestId");

        auditService.recordDenied(
                "ACCESS_DENIED",
                "ENDPOINT",
                request.getRequestURI(),
                AuditOutcome.DENIED,
                "FORBIDDEN",
                null,
                Map.of("path", request.getRequestURI())
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .code("ACCESS_DENIED")
                .message("Bạn không có quyền thực hiện thao tác này.")
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

