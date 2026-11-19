package com.fpt.ems.be.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tóm tắt của Kỹ thuật viên đang hoạt động dành cho Manager phân công công việc")
public class TechnicianSummaryResponse {

    @Schema(description = "Mã định danh Kỹ thuật viên", example = "4")
    private Long id;

    @Schema(description = "Họ và tên Kỹ thuật viên", example = "Le Van Technician A")
    private String fullName;
}

