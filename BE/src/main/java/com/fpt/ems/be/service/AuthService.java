package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.auth.LoginResponse;
import com.fpt.ems.be.dto.auth.MeResponse;
import com.fpt.ems.be.dto.auth.RegisterRequest;
import com.fpt.ems.be.dto.auth.RegisterResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.Role;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.RoleRepository;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.util.IpRateLimiter;
import com.fpt.ems.be.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final IpRateLimiter ipRateLimiter;

    @Value("${app.registration.enabled:true}")
    private boolean registrationEnabled;

    @Value("${app.registration.require-approval:true}")
    private boolean requireApproval;

    @Value("${app.registration.rate-limit-per-hour:5}")
    private int rateLimitPerHour;

    private static final String DUMMY_BCRYPT_HASH = "$2a$10$w3U6mIq9vjF2oXlqO7Kz3eR9CvhW5Y3gZ4Xb1Q2m8L5Jk9Vp6HsTu";

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();

        // Fixed order: query username first, then lower(email) without using SQL OR
        AppUser user = appUserRepository.findByUsername(identifier).orElse(null);
        if (user == null) {
            user = appUserRepository.findByEmailIgnoreCase(identifier.toLowerCase()).orElse(null);
        }

        // Security requirement: Generic message for invalid credentials, do not leak whether username/email exists
        if (user == null) {
            // Constant-time dummy computation to mitigate timing attacks
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            auditService.recordDenied("LOGIN", "USER", identifier, AuditOutcome.FAILED, "BAD_CREDENTIALS", null, null);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        Instant now = Instant.now();

        // Check administrative LOCKED status: Return generic 401 error, audit LOCKED
        if (user.getStatus() == UserStatus.LOCKED) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            auditService.recordDeniedWithActor("LOGIN", "USER", String.valueOf(user.getUserId()),
                    AuditOutcome.FAILED, "LOCKED", null, null, user.getUserId(), user.getRole().getName().name());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // Check temporary lock from repeated failed attempts: Return generic 401 error, audit LOCKED
        if (user.getLockedUntil() != null) {
            if (now.isBefore(user.getLockedUntil())) {
                passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
                auditService.recordDeniedWithActor("LOGIN", "USER", String.valueOf(user.getUserId()),
                        AuditOutcome.FAILED, "LOCKED", null, null, user.getUserId(), user.getRole().getName().name());
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
            } else {
                // Lock expired: reset lock and failed count
                user.setLockedUntil(null);
                user.setFailedLoginCount(0);
                user.setLastFailedAt(null);
            }
        }

        // Check PENDING_APPROVAL and REJECTED: Return generic 401 error, never leak status
        if (user.getStatus() == UserStatus.PENDING_APPROVAL || user.getStatus() == UserStatus.REJECTED) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            auditService.recordDeniedWithActor("LOGIN", "USER", String.valueOf(user.getUserId()),
                    AuditOutcome.FAILED, "BAD_CREDENTIALS", null, null, user.getUserId(), user.getRole().getName().name());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Check 5-minute sliding window
            if (user.getLastFailedAt() != null && Duration.between(user.getLastFailedAt(), now).toMinutes() >= 5) {
                user.setFailedLoginCount(1);
            } else {
                user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            }
            user.setLastFailedAt(now);

            // If reached 5 failed attempts in 5 minutes -> lock for 15 minutes, return generic 401 error
            if (user.getFailedLoginCount() >= 5) {
                user.setLockedUntil(now.plus(15, ChronoUnit.MINUTES));
                appUserRepository.save(user);
                auditService.recordDeniedWithActor("LOGIN", "USER", String.valueOf(user.getUserId()),
                        AuditOutcome.FAILED, "LOCKED", null, null, user.getUserId(), user.getRole().getName().name());
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
            }

            appUserRepository.save(user);
            auditService.recordDeniedWithActor("LOGIN", "USER", String.valueOf(user.getUserId()),
                    AuditOutcome.FAILED, "BAD_CREDENTIALS", null, null, user.getUserId(), user.getRole().getName().name());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // Reset failed login counter on successful authentication
        user.setFailedLoginCount(0);
        user.setLastFailedAt(null);
        user.setLockedUntil(null);
        appUserRepository.save(user);

        String roleName = user.getRole().getName().name();
        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), roleName);

        auditService.recordWithActor("LOGIN", "USER", String.valueOf(user.getUserId()), AuditOutcome.SUCCESS, "SUCCESS",
                null, Map.of("userId", user.getUserId(), "username", user.getUsername(), "roleName", roleName),
                user.getUserId(), roleName);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationSeconds())
                .user(LoginResponse.UserSummary.builder()
                        .id(user.getUserId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .role(roleName)
                        .build())
                .build();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp) {
        // 1. Check feature toggle
        if (!registrationEnabled) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "REGISTRATION_DISABLED", "Chức năng đăng ký tài khoản tạm thời không khả dụng.");
        }

        // 2. Application-level IP Rate Limiter (5 requests / hour / IP)
        if (!ipRateLimiter.tryAcquire(clientIp, rateLimitPerHour)) {
            auditService.recordDenied("REGISTER", "USER", clientIp, AuditOutcome.FAILED, "REGISTER_RATE_LIMITED", null, null);
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau 1 giờ.");
        }

        // 3. Password policy validation
        validatePasswordPolicy(request.password(), request.confirmPassword(), request.email(), request.fullName());

        String normalizedEmail = request.email().trim().toLowerCase();
        String maskedEmail = maskEmail(normalizedEmail);

        String successMessage = requireApproval
                ? "Yêu cầu đăng ký đã được ghi nhận. Vui lòng chờ quản trị viên phê duyệt."
                : "Đăng ký tài khoản thành công. Bạn có thể đăng nhập.";

        // 4. Anti-enumeration: Check if email already exists (including ACTIVE, LOCKED, PENDING_APPROVAL, REJECTED)
        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail) || appUserRepository.existsByUsername(normalizedEmail)) {
            // Perform dummy BCrypt hash to ensure indistinguishable timing
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            auditService.recordDenied("REGISTER", "USER", maskedEmail, AuditOutcome.FAILED, "REGISTER_DUPLICATE_EMAIL", null, null);
            // Return exactly the same HTTP response as successful registration
            return new RegisterResponse(successMessage, requireApproval);
        }

        // 5. Server always assigns role EQUIPMENT_USER
        Role equipmentUserRole = roleRepository.findByName(RoleName.EQUIPMENT_USER)
                .orElseGet(() -> {
                    log.warn("Role EQUIPMENT_USER not found in DB. Auto-provisioning core role...");
                    return roleRepository.save(Role.builder()
                            .name(RoleName.EQUIPMENT_USER)
                            .build());
                });

        // 6. Initial status
        UserStatus initialStatus = requireApproval ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE;

        AppUser newUser = AppUser.builder()
                .username(normalizedEmail)
                .email(normalizedEmail)
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(initialStatus)
                .role(equipmentUserRole)
                .failedLoginCount(0)
                .build();

        AppUser savedUser = appUserRepository.save(newUser);

        // 7. Audit log
        auditService.record("USER_REGISTERED", "USER", String.valueOf(savedUser.getUserId()),
                AuditOutcome.SUCCESS, "SUCCESS",
                null,
                Map.of(
                        "userId", savedUser.getUserId(),
                        "email", maskedEmail,
                        "role", RoleName.EQUIPMENT_USER.name(),
                        "status", initialStatus.name()
                )
        );

        return new RegisterResponse(successMessage, requireApproval);
    }

    private void validatePasswordPolicy(String password, String confirmPassword, String email, String fullName) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Mật khẩu xác nhận không khớp.");
        }

        if (password.length() < 12) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu phải có độ dài tối thiểu 12 ký tự.");
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUpper || !hasLower || !hasDigit) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số.");
        }

        String pwdLower = password.toLowerCase();

        // Check against email and email prefix
        if (email != null && !email.isBlank()) {
            String emailLower = email.toLowerCase().trim();
            if (pwdLower.contains(emailLower)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu không được chứa email.");
            }
            int atIndex = emailLower.indexOf('@');
            if (atIndex >= 3) {
                String prefix = emailLower.substring(0, atIndex);
                if (pwdLower.contains(prefix)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu không được chứa tên tài khoản email.");
                }
            }
        }

        // Check against full name and words in full name (if length >= 3)
        if (fullName != null && !fullName.isBlank()) {
            String nameLower = fullName.toLowerCase().trim();
            if (pwdLower.contains(nameLower)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu không được chứa họ tên.");
            }
            String[] words = nameLower.split("\\s+");
            for (String word : words) {
                if (word.length() >= 3 && pwdLower.contains(word)) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Mật khẩu không được chứa họ tên của bạn.");
                }
            }
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    @Transactional
    public void logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            auditService.record("LOGOUT", "USER", String.valueOf(principal.getUserId()),
                    AuditOutcome.SUCCESS, "SUCCESS", null,
                    Map.of("userId", principal.getUserId(), "username", principal.getUsername()));
        }
    }

    public MeResponse getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chưa đăng nhập");
        }

        AppUser user = appUserRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại"));

        String role = user.getRole().getName().name();
        List<String> permissions = resolvePermissions(user.getRole().getName());
        List<String> menus = resolveMenus(user.getRole().getName());

        return MeResponse.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(role)
                .permissions(permissions)
                .menus(menus)
                .build();
    }

    private List<String> resolvePermissions(RoleName role) {
        return switch (role) {
            case ADMIN -> List.of("USERS_MANAGE", "EQUIPMENT_MANAGE", "LOCATIONS_MANAGE", "AUDIT_VIEW");
            case MANAGER -> List.of("WORK_ORDERS_MANAGE", "INCIDENTS_MANAGE", "EQUIPMENT_VIEW", "DASHBOARD_VIEW", "PLANS_MANAGE");
            case TECHNICIAN -> List.of("WORK_ORDERS_EXECUTE", "EQUIPMENT_VIEW", "INCIDENTS_PROGRESS", "STATUS_PROPOSE");
            case EQUIPMENT_USER -> List.of("INCIDENTS_CREATE", "INCIDENTS_OWN_VIEW", "EQUIPMENT_VIEW");
        };
    }

    private List<String> resolveMenus(RoleName role) {
        return switch (role) {
            case ADMIN -> List.of("/users", "/equipment", "/locations", "/audit-logs");
            case MANAGER -> List.of("/dashboard", "/work-orders", "/incidents", "/equipment", "/plans");
            case TECHNICIAN -> List.of("/my-work-orders", "/equipment");
            case EQUIPMENT_USER -> List.of("/report-incident", "/my-incidents", "/equipment");
        };
    }
}
