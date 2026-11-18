package com.fpt.ems.be.dto.auth;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeResponse {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private List<String> permissions;
    private List<String> menus;
}

