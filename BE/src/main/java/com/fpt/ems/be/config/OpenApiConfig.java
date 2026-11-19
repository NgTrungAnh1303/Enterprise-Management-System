package com.fpt.ems.be.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Equipment Maintenance & Service Scheduling System (P05) API",
                version = "v1.0",
                description = "REST API for Equipment Maintenance and Asset Integrity Management",
                contact = @Contact(name = "EMS Engineering Team")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token. Enter your token generated from /api/v1/auth/login"
)
public class OpenApiConfig {
}

