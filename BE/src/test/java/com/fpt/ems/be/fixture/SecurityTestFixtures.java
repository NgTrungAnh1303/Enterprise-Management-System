package com.fpt.ems.be.fixture;

import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import com.fpt.ems.be.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityTestFixtures {

    public static final Long ADMIN_ID = 1L;
    public static final String ADMIN_USERNAME = "ADMIN01";
    public static final String ADMIN_ROLE = "ADMIN";

    public static final Long MGR01_ID = 2L;
    public static final String MGR01_USERNAME = "MGR01";
    public static final String MGR01_ROLE = "MANAGER";

    public static final Long MGR02_ID = 3L;
    public static final String MGR02_USERNAME = "MGR02";
    public static final String MGR02_ROLE = "MANAGER";

    public static final Long TECH_A_ID = 4L;
    public static final String TECH_A_USERNAME = "TECH_A";
    public static final String TECH_A_ROLE = "TECHNICIAN";

    public static final Long TECH_B_ID = 5L;
    public static final String TECH_B_USERNAME = "TECH_B";
    public static final String TECH_B_ROLE = "TECHNICIAN";

    public static final Long USER01_ID = 6L;
    public static final String USER01_USERNAME = "USER01";
    public static final String USER01_ROLE = "EQUIPMENT_USER";

    public static final Long USER02_ID = 7L;
    public static final String USER02_USERNAME = "USER02";
    public static final String USER02_ROLE = "EQUIPMENT_USER";

    public static final String DEFAULT_PASSWORD = "Demo@123456";

    public static final Long EQ01_ID = 1L;
    public static final Long EQ02_ID = 2L;
    public static final Long EQ03_ID = 3L;
    public static final Long EQ04_ID = 4L;
    public static final Long EQ05_ID = 5L;

    public static final Long WO_A_ID = 1L;
    public static final String WO_A_CODE = "WO_A";

    public static final Long WO_B_ID = 2L;
    public static final String WO_B_CODE = "WO_B";

    public static final Long WO_P_ID = 3L;
    public static final String WO_P_CODE = "WO_P";

    public static final Long WO_C_ID = 4L;
    public static final String WO_C_CODE = "WO_C";

    public static final Long WO_V_ID = 5L;
    public static final String WO_V_CODE = "WO_V";

    private final JwtUtils jwtUtils;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogRepository auditLogRepository;
    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository assignmentRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final EquipmentRepository equipmentRepository;

    public SecurityTestFixtures(JwtUtils jwtUtils,
                                JdbcTemplate jdbcTemplate,
                                AuditLogRepository auditLogRepository,
                                MaintenanceWorkOrderRepository workOrderRepository,
                                WorkOrderAssignmentRepository assignmentRepository,
                                InspectionResultRepository inspectionResultRepository,
                                EquipmentRepository equipmentRepository) {
        this.jwtUtils = jwtUtils;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogRepository = auditLogRepository;
        this.workOrderRepository = workOrderRepository;
        this.assignmentRepository = assignmentRepository;
        this.inspectionResultRepository = inspectionResultRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public String tokenAdmin() {
        return jwtUtils.generateToken(ADMIN_ID, ADMIN_USERNAME, ADMIN_ROLE);
    }

    public String tokenManager1() {
        return jwtUtils.generateToken(MGR01_ID, MGR01_USERNAME, MGR01_ROLE);
    }

    public String tokenManager2() {
        return jwtUtils.generateToken(MGR02_ID, MGR02_USERNAME, MGR02_ROLE);
    }

    public String tokenTechA() {
        return jwtUtils.generateToken(TECH_A_ID, TECH_A_USERNAME, TECH_A_ROLE);
    }

    public String tokenTechB() {
        return jwtUtils.generateToken(TECH_B_ID, TECH_B_USERNAME, TECH_B_ROLE);
    }

    public String tokenUser1() {
        return jwtUtils.generateToken(USER01_ID, USER01_USERNAME, USER01_ROLE);
    }

    public String tokenUser2() {
        return jwtUtils.generateToken(USER02_ID, USER02_USERNAME, USER02_ROLE);
    }

    public String tokenCustom(Long userId, String username, String role) {
        return jwtUtils.generateToken(userId, username, role);
    }

    public String tokenExpired(Long userId, String username, String role) {
        Date now = new Date(System.currentTimeMillis() - 7200 * 1000);
        Date expired = new Date(System.currentTimeMillis() - 3600 * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expired)
                .signWith(jwtUtils.getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String tokenWithInvalidSignature(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1800 * 1000);
        
        javax.crypto.SecretKey fakeKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "an_invalid_key_for_testing_tampered_jwt_signature_1234567890".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(fakeKey, Jwts.SIG.HS256)
                .compact();
    }

    
    public void ensureWoV() {
        Optional<MaintenanceWorkOrder> existing = workOrderRepository.findById(WO_V_ID);
        if (existing.isPresent()) {
            jdbcTemplate.update("UPDATE maintenance_work_order SET status = 'SUBMITTED' WHERE wo_id = ?", WO_V_ID);
            return;
        }

        
        jdbcTemplate.update(
                "INSERT INTO maintenance_work_order (wo_id, wo_code, equipment_id, plan_id, type, period, status, created_at) " +
                        "VALUES (?, ?, 5, 5, 'PERIODIC', '2026-Q1', 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '4 days') " +
                        "ON CONFLICT (wo_id) DO UPDATE SET status = 'SUBMITTED'",
                WO_V_ID, WO_V_CODE
        );

        
        jdbcTemplate.update(
                "INSERT INTO work_order_assignment (assignment_id, wo_id, technician_id, assigned_by, assigned_at, is_current) " +
                        "VALUES (105, ?, ?, 2, CURRENT_TIMESTAMP - INTERVAL '4 days', TRUE) " +
                        "ON CONFLICT (assignment_id) DO NOTHING",
                WO_V_ID, TECH_A_ID
        );

        
        jdbcTemplate.update(
                "INSERT INTO inspection_result (result_id, wo_id, checklist_id, done, condition, defect, work_done, entered_by, entered_at) " +
                        "VALUES (107, ?, 7, TRUE, 'Bình thường', 'Không có', 'Đã kiểm tra nguồn đèn', ?, CURRENT_TIMESTAMP - INTERVAL '1 day') " +
                        "ON CONFLICT (result_id) DO NOTHING",
                WO_V_ID, TECH_A_ID
        );
        jdbcTemplate.update(
                "INSERT INTO inspection_result (result_id, wo_id, checklist_id, done, condition, defect, work_done, entered_by, entered_at) " +
                        "VALUES (108, ?, 8, TRUE, 'Bình thường', 'Không có', 'Đã hiệu chuẩn', ?, CURRENT_TIMESTAMP - INTERVAL '1 day') " +
                        "ON CONFLICT (result_id) DO NOTHING",
                WO_V_ID, TECH_A_ID
        );
        jdbcTemplate.update(
                "INSERT INTO inspection_result (result_id, wo_id, checklist_id, done, condition, defect, work_done, entered_by, entered_at) " +
                        "VALUES (109, ?, 9, TRUE, 'Sạch sẽ', 'Không có', 'Đã vệ sinh buồng đo', ?, CURRENT_TIMESTAMP - INTERVAL '1 day') " +
                        "ON CONFLICT (result_id) DO NOTHING",
                WO_V_ID, TECH_A_ID
        );
    }

    /**
     * 
     */
    public Optional<AuditLog> findLatestAuditLog(String action) {
        List<AuditLog> logs = auditLogRepository.findByActionOrderByOccurredAtDesc(action);
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }

    /**
     * 
     */
    public Optional<AuditLog> findLatestAuditLog(String action, String targetType, String targetId) {
        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> action.equals(l.getAction()))
                .filter(l -> targetType == null || targetType.equals(l.getTargetType()))
                .filter(l -> targetId == null || targetId.equals(l.getTargetId()))
                .sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()))
                .toList();
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }
}
