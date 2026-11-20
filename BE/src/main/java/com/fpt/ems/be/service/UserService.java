package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.user.CreateUserRequest;
import com.fpt.ems.be.dto.user.TechnicianSummaryResponse;
import com.fpt.ems.be.dto.user.UpdateUserRequest;
import com.fpt.ems.be.dto.user.UserResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.Role;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.RoleRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(String q, Integer roleId, UserStatus status, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String searchPattern = "%" + q.trim().toLowerCase() + "%";
                Predicate usernameLike = cb.like(cb.lower(root.get("username")), searchPattern);
                Predicate fullNameLike = cb.like(cb.lower(root.get("fullName")), searchPattern);
                predicates.add(cb.or(usernameLike, fullNameLike));
            }

            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("roleId"), roleId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AppUser> usersPage = userRepository.findAll(spec, pageable);
        List<UserResponse> items = usersPage.getContent().stream()
                .map(UserResponse::fromEntity)
                .toList();

        return PageResponse.<UserResponse>builder()
                .items(items)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .total(usersPage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TechnicianSummaryResponse> getActiveTechnicians() {
        return userRepository.findActiveTechnicians(UserStatus.ACTIVE).stream()
                .map(u -> TechnicianSummaryResponse.builder()
                        .id(u.getUserId())
                        .fullName(u.getFullName())
                        .build())
                .toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Tên đăng nhập đã tồn tại: " + username);
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Vai trò không hợp lệ: " + request.getRoleId()));

        AppUser newUser = AppUser.builder()
                .username(username)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .role(role)
                .failedLoginCount(0)
                .build();

        AppUser savedUser = userRepository.save(newUser);

        auditService.record("USER_CREATE", "USER", String.valueOf(savedUser.getUserId()),
                AuditOutcome.SUCCESS, "SUCCESS",
                null,
                Map.of(
                        "userId", savedUser.getUserId(),
                        "username", savedUser.getUsername(),
                        "fullName", savedUser.getFullName(),
                        "roleName", role.getName().name(),
                        "status", savedUser.getStatus().name()
                )
        );

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse approveUser(Long targetUserId, Long adminUserId) {
        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại"));

        if (targetUser.getStatus() != UserStatus.PENDING_APPROVAL) {
            auditService.recordDenied("USER_APPROVE", "USER", String.valueOf(targetUserId),
                    AuditOutcome.FAILED, "STATE_VIOLATION",
                    Map.of("currentStatus", targetUser.getStatus().name()), null);
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    "Chỉ có thể phê duyệt tài khoản ở trạng thái PENDING_APPROVAL. Trạng thái hiện tại: " + targetUser.getStatus());
        }

        targetUser.setStatus(UserStatus.ACTIVE);
        targetUser.setFailedLoginCount(0);
        targetUser.setLockedUntil(null);
        targetUser.setLastFailedAt(null);

        AppUser saved = userRepository.save(targetUser);

        auditService.record("USER_APPROVED", "USER", String.valueOf(saved.getUserId()),
                AuditOutcome.SUCCESS, "SUCCESS",
                Map.of("oldStatus", "PENDING_APPROVAL"),
                Map.of("newStatus", "ACTIVE", "approvedBy", adminUserId)
        );

        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse rejectUser(Long targetUserId, Long adminUserId) {
        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại"));

        if (targetUser.getStatus() != UserStatus.PENDING_APPROVAL) {
            auditService.recordDenied("USER_REJECT", "USER", String.valueOf(targetUserId),
                    AuditOutcome.FAILED, "STATE_VIOLATION",
                    Map.of("currentStatus", targetUser.getStatus().name()), null);
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                    "Chỉ có thể từ chối tài khoản ở trạng thái PENDING_APPROVAL. Trạng thái hiện tại: " + targetUser.getStatus());
        }

        targetUser.setStatus(UserStatus.REJECTED);
        AppUser saved = userRepository.save(targetUser);

        auditService.record("USER_REJECTED", "USER", String.valueOf(saved.getUserId()),
                AuditOutcome.SUCCESS, "SUCCESS",
                Map.of("oldStatus", "PENDING_APPROVAL"),
                Map.of("newStatus", "REJECTED", "rejectedBy", adminUserId)
        );

        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUser(Long targetUserId, UpdateUserRequest request, Long currentAdminUserId) {
        AppUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại"));

        if (targetUserId.equals(currentAdminUserId)) {
            if (request.getStatus() == UserStatus.LOCKED) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "CANNOT_LOCK_SELF", "Admin không thể tự khóa tài khoản của chính mình.");
            }
            if (request.getRoleId() != null && !request.getRoleId().equals(targetUser.getRole().getRoleId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "CANNOT_DEMOTE_SELF", "Admin không thể tự thay đổi vai trò của chính mình.");
            }
        }

        String oldRoleName = targetUser.getRole().getName().name();
        String oldStatus = targetUser.getStatus().name();

        boolean roleChanged = false;
        if (request.getRoleId() != null && !request.getRoleId().equals(targetUser.getRole().getRoleId())) {
            Role newRole = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "Vai trò không hợp lệ: " + request.getRoleId()));
            targetUser.setRole(newRole);
            roleChanged = true;
        }

        boolean statusChanged = false;
        String auditAction = "USER_UPDATE";
        if (request.getStatus() != null && request.getStatus() != targetUser.getStatus()) {
            UserStatus targetStatus = request.getStatus();

            if (targetUser.getStatus() == UserStatus.PENDING_APPROVAL) {
                if (targetStatus == UserStatus.ACTIVE) {
                    auditAction = "USER_APPROVED";
                } else if (targetStatus == UserStatus.REJECTED) {
                    auditAction = "USER_REJECTED";
                } else {
                    auditService.recordDenied("USER_UPDATE", "USER", String.valueOf(targetUserId),
                            AuditOutcome.FAILED, "STATE_VIOLATION",
                            Map.of("currentStatus", oldStatus, "targetStatus", targetStatus.name()), null);
                    throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                            "Tài khoản PENDING_APPROVAL chỉ có thể chuyển sang ACTIVE hoặc REJECTED.");
                }
            } else {
                if (targetStatus == UserStatus.REJECTED || targetStatus == UserStatus.PENDING_APPROVAL) {
                    auditService.recordDenied("USER_UPDATE", "USER", String.valueOf(targetUserId),
                            AuditOutcome.FAILED, "STATE_VIOLATION",
                            Map.of("currentStatus", oldStatus, "targetStatus", targetStatus.name()), null);
                    throw new BusinessException(HttpStatus.CONFLICT, "STATE_VIOLATION",
                            "Không thể chuyển tài khoản từ trạng thái " + oldStatus + " sang " + targetStatus);
                }
            }

            targetUser.setStatus(targetStatus);
            if (targetStatus == UserStatus.ACTIVE) {
                targetUser.setFailedLoginCount(0);
                targetUser.setLockedUntil(null);
                targetUser.setLastFailedAt(null);
            }
            statusChanged = true;
        }

        AppUser updated = userRepository.save(targetUser);

        if (roleChanged) {
            auditService.record("ROLE_CHANGE", "USER", String.valueOf(updated.getUserId()),
                    AuditOutcome.SUCCESS, "SUCCESS",
                    Map.of("roleName", oldRoleName),
                    Map.of("roleName", updated.getRole().getName().name())
            );
        }

        if (statusChanged) {
            auditService.record(auditAction, "USER", String.valueOf(updated.getUserId()),
                    AuditOutcome.SUCCESS, "STATUS_UPDATED",
                    Map.of("status", oldStatus),
                    Map.of("status", updated.getStatus().name())
            );
        }

        return UserResponse.fromEntity(updated);
    }
}
