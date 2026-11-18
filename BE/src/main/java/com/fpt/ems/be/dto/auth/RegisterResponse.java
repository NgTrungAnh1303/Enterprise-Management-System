package com.fpt.ems.be.dto.auth;

public record RegisterResponse(
        String message,
        boolean requireApproval
) {
}
