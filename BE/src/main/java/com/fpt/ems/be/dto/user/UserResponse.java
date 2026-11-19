package com.fpt.ems.be.dto.user;

import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.enums.UserStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Integer roleId;
    private String roleName;
    private UserStatus status;
    private Integer failedLoginCount;
    private Instant lockedUntil;
    private Instant createdAt;

    public static UserResponse fromEntity(AppUser user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName().name() : null)
                .status(user.getStatus())
                .failedLoginCount(user.getFailedLoginCount())
                .lockedUntil(user.getLockedUntil())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

