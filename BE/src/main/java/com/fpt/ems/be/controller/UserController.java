package com.fpt.ems.be.controller;

import com.fpt.ems.be.dto.common.PageResponse;
import com.fpt.ems.be.dto.user.CreateUserRequest;
import com.fpt.ems.be.dto.user.TechnicianSummaryResponse;
import com.fpt.ems.be.dto.user.UpdateUserRequest;
import com.fpt.ems.be.dto.user.UserResponse;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints quản lý tài khoản và phân quyền người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Danh sách người dùng",
            description = "Lấy danh sách người dùng có phân trang và bộ lọc theo từ khóa, roleId, status. Chỉ ADMIN được phép.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN")
            }
    )
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserResponse> response = userService.getUsers(q, roleId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Danh sách Kỹ thuật viên đang hoạt động",
            description = "Lấy danh sách ID và họ tên của các Kỹ thuật viên có trạng thái ACTIVE để phục vụ phân công Work Order. Chỉ MANAGER được phép.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền MANAGER")
            }
    )
    public ResponseEntity<java.util.List<TechnicianSummaryResponse>> getActiveTechnicians() {
        java.util.List<TechnicianSummaryResponse> response = userService.getActiveTechnicians();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo tài khoản mới",
            description = "Tạo người dùng mới với vai trò được chỉ định. Mật khẩu yêu cầu tối thiểu 8 ký tự gồm chữ và số. Chỉ ADMIN được phép.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Tạo tài khoản thành công"),
                    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc mật khẩu không đủ mạnh"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN"),
                    @ApiResponse(responseCode = "409", description = "Trùng tên đăng nhập")
            }
    )
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Phê duyệt tài khoản đăng ký (Ngoài SRS - ADMIN)",
            description = "Chuyển tài khoản từ PENDING_APPROVAL sang ACTIVE. Chỉ áp dụng cho tài khoản PENDING_APPROVAL. Ghi nhận audit USER_APPROVED.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Phê duyệt thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng"),
                    @ApiResponse(responseCode = "409", description = "Trạng thái người dùng không phải PENDING_APPROVAL")
            }
    )
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserResponse response = userService.approveUser(id, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Từ chối tài khoản đăng ký (Ngoài SRS - ADMIN)",
            description = "Chuyển tài khoản từ PENDING_APPROVAL sang REJECTED. Chỉ áp dụng cho tài khoản PENDING_APPROVAL. Ghi nhận audit USER_REJECTED.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Từ chối thành công"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng"),
                    @ApiResponse(responseCode = "409", description = "Trạng thái người dùng không phải PENDING_APPROVAL")
            }
    )
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserResponse response = userService.rejectUser(id, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật vai trò hoặc trạng thái tài khoản",
            description = "Thay đổi roleId hoặc khóa/mở khóa tài khoản người dùng. Admin không được phép tự khóa hoặc hạ quyền chính mình.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN hoặc cố tình tự khóa/hạ quyền"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng"),
                    @ApiResponse(responseCode = "409", description = "Xung đột quy tắc chuyển đổi trạng thái")
            }
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserResponse response = userService.updateUser(id, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }
}
