package com.fpt.ems.be.dto.user;

import com.fpt.ems.be.model.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    private Integer roleId;
    private UserStatus status;
}

