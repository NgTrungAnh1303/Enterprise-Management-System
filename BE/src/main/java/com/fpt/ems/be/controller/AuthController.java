package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.auth.LoginResponse;
import com.fpt.ems.be.dto.auth.MeResponse;
import com.fpt.ems.be.dto.auth.RegisterRequest;
import com.fpt.ems.be.dto.auth.RegisterResponse;
import com.fpt.ems.be.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints phục vụ xác thực, đăng nhập, đăng ký và thông tin phiên làm việc")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập hệ thống",
            description = "Xác thực người dùng bằng username/email và password, trả về JWT access token. Sai 5 lần trong 5 phút sẽ khóa tạm 15 phút.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT"),
                    @ApiResponse(responseCode = "401", description = "Tên đăng nhập hoặc mật khẩu không đúng"),
                    @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa hoặc tạm khóa")
            }
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Đăng ký tài khoản người dùng mới (Ngoài SRS - Public)",
            description = "Đăng ký tài khoản tự phục vụ với vai trò EQUIPMENT_USER. Hỗ trợ phê duyệt PENDING_APPROVAL, chống dò email và rate limit.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Đăng ký thành công hoặc ghi nhận yêu cầu"),
                    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mật khẩu không đủ mạnh"),
                    @ApiResponse(responseCode = "429", description = "Vượt quá giới hạn tần suất đăng ký (5 lần/giờ/IP)")
            }
    )
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = resolveClientIp(servletRequest);
        RegisterResponse response = authService.register(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Đăng xuất",
            description = "Ghi nhận sự kiện đăng xuất và hủy phiên làm việc phía client",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Đăng xuất thành công, không trả nội dung")
            }
    )
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin tài khoản hiện tại",
            description = "Trả về thông tin cá nhân, vai trò, quyền hạn và danh sách menu giao diện của người dùng đang đăng nhập",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thành công"),
                    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
            }
    )
    public ResponseEntity<MeResponse> getMe() {
        MeResponse response = authService.getMe();
        return ResponseEntity.ok(response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
