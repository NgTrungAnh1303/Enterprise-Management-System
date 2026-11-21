package com.fpt.ems.be.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.config.RequestIdFilter;
import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.incident.IncidentCreateRequest;
import com.fpt.ems.be.dto.plan.MaintenancePlanRequest;
import com.fpt.ems.be.dto.statusrequest.CreateStatusRequestDto;
import com.fpt.ems.be.dto.statusrequest.StatusRequestDecisionDto;
import com.fpt.ems.be.dto.user.CreateUserRequest;
import com.fpt.ems.be.dto.user.UpdateUserRequest;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.fixture.SecurityTestFixtures;
import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase3SecurityIntegrationTest {

    private static PostgreSQLContainer<?> postgresContainer;
    private static String jdbcUrl;
    private static String migrationUsername;
    private static String migrationPassword;
    private static final String APP_USER_PASSWORD = "test_app_user_pass_123";
    private static boolean isLocalClusterStarted = false;
    private static File localDataDir;
    private static int localPort;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityTestFixtures fixtures;

    @Autowired
    private MaintenanceWorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderAssignmentRepository assignmentRepository;

    @Autowired
    private InspectionResultRepository inspectionResultRepository;

    @Autowired
    private WorkOrderAcceptanceRepository acceptanceRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private MaintenancePlanRepository planRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeAll
    static void setupDatabase() throws Exception {
        boolean dockerAvailable = false;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            dockerAvailable = false;
        }

        if (dockerAvailable) {
            postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ems_test_db")
                    .withUsername("postgres")
                    .withPassword("postgres");
            postgresContainer.start();
            jdbcUrl = postgresContainer.getJdbcUrl();
            migrationUsername = postgresContainer.getUsername();
            migrationPassword = postgresContainer.getPassword();
        } else {
            try (ServerSocket s = new ServerSocket(0)) {
                localPort = s.getLocalPort();
            }
            jdbcUrl = "jdbc:postgresql://127.0.0.1:" + localPort + "/postgres";
            migrationUsername = "postgres";
            migrationPassword = "postgres";

            localDataDir = new File("target/test-pg-phase3-" + System.currentTimeMillis());
            String psqlBinDir = "C:\\Program Files\\PostgreSQL\\16\\bin";

            Process initProcess = new ProcessBuilder(
                    psqlBinDir + "\\initdb.exe",
                    "-E", "UTF8",
                    "-D", localDataDir.getAbsolutePath(),
                    "-U", "postgres",
                    "-A", "trust"
            ).inheritIO().start();
            assertTrue(initProcess.waitFor(30, TimeUnit.SECONDS), "initdb timed out");

            File logFile = new File(localDataDir, "postgres.log");
            Process startProcess = new ProcessBuilder(
                    psqlBinDir + "\\pg_ctl.exe",
                    "-D", localDataDir.getAbsolutePath(),
                    "-l", logFile.getAbsolutePath(),
                    "-o", "-p " + localPort,
                    "-w",
                    "start"
            ).inheritIO().start();
            assertTrue(startProcess.waitFor(20, TimeUnit.SECONDS), "pg_ctl start timed out");
            isLocalClusterStarted = true;

            long deadline = System.currentTimeMillis() + 15000;
            boolean connected = false;
            while (System.currentTimeMillis() < deadline) {
                try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
                    connected = true;
                    break;
                } catch (Exception e) {
                    Thread.sleep(500);
                }
            }
            assertTrue(connected, "Failed to connect to local PostgreSQL cluster");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, migrationUsername, migrationPassword)
                .locations("classpath:db/migration", "classpath:db/seed-dev")
                .placeholders(Map.of("app_user_password", APP_USER_PASSWORD))
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }

    @AfterAll
    static void tearDownDatabase() {
        if (postgresContainer != null) {
            postgresContainer.stop();
        } else if (isLocalClusterStarted && localDataDir != null) {
            try {
                String psqlBinDir = "C:\\Program Files\\PostgreSQL\\16\\bin";
                new ProcessBuilder(
                        psqlBinDir + "\\pg_ctl.exe",
                        "-D", localDataDir.getAbsolutePath(),
                        "stop", "-m", "immediate"
                ).inheritIO().start().waitFor(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> "app_user");
        registry.add("spring.datasource.password", () -> APP_USER_PASSWORD);
    }

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilter(requestIdFilter)
                .build();

        fixtures.ensureWoV();
    }

    

    @Test
    @Order(1)
    @DisplayName("SEC-TC-520 TECH_A cập nhật 1 mục checklist của WO_A (được giao)")
    void sec_tc_520_technician_can_update_checklist_on_assigned_wo() throws Exception {
        String token = fixtures.tokenTechA();

        UpdateInspectionResultsRequest request = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(1L)
                                .done(true)
                                .condition("Rotor cân bằng tốt, không có độ rơ")
                                .defect("Không có lỗi")
                                .workDone("Đã kiểm tra bằng tay và dụng cụ đo")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/1/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.woId").value(1))
                .andExpect(jsonPath("$.inspectionResults", hasSize(greaterThanOrEqualTo(1))));

        
        InspectionResult saved = inspectionResultRepository
                .findByWorkOrder_WoIdAndChecklist_ChecklistId(1L, 1L).orElse(null);
        assertNotNull(saved);
        assertTrue(saved.getDone());
        assertEquals("Rotor cân bằng tốt, không có độ rơ", saved.getCondition());
        assertEquals(SecurityTestFixtures.TECH_A_ID, saved.getEnteredBy().getUserId());

        
        AuditLog log = fixtures.findLatestAuditLog("WO_RESULT_UPDATE", "WORK_ORDER", "1")
                .orElse(null);
        assertNotNull(log, "AuditLog cho WO_RESULT_UPDATE phải tồn tại");
        assertEquals(AuditOutcome.SUCCESS, log.getOutcome());
        assertEquals(SecurityTestFixtures.TECH_A_ID, log.getActorId());
        assertEquals("TECHNICIAN", log.getActorRole());
    }

    @Test
    @Order(2)
    @DisplayName("SEC-TC-501 TECH_A cập nhật WO_B (giao TECH_B)")
    void sec_tc_501_technician_cannot_update_unassigned_wo() throws Exception {
        String token = fixtures.tokenTechA();

        UpdateInspectionResultsRequest request = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(1L)
                                .done(true)
                                .condition("Xâm phạm trái phép")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/2/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("WO_NOT_ASSIGNED"));

        
        Optional<InspectionResult> result = inspectionResultRepository
                .findByWorkOrder_WoIdAndChecklist_ChecklistId(2L, 1L);
        assertTrue(result.isEmpty(), "WO_B không được phép có kết quả từ TECH_A");

        
        AuditLog log = fixtures.findLatestAuditLog("ACCESS_DENIED", "WORK_ORDER", "2")
                .orElse(null);
        assertNotNull(log, "AuditLog ACCESS_DENIED phải được ghi nhận");
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
        assertEquals("WO_NOT_ASSIGNED", log.getReasonCode());
        assertEquals(SecurityTestFixtures.TECH_A_ID, log.getActorId());
    }

    @Test
    @Order(3)
    @DisplayName("SEC-TC-524 Manager đổi WO_A từ TECH_A sang TECH_B; TECH_A gửi cập nhật bằng phiên cũ")
    void sec_tc_524_reassigned_technician_cannot_update_wo_using_old_session() throws Exception {
        String mgrToken = fixtures.tokenManager1();
        String techAToken = fixtures.tokenTechA();

        
        AssignWorkOrderRequest assignReq = AssignWorkOrderRequest.builder()
                .technicianId(SecurityTestFixtures.TECH_B_ID)
                .build();

        mockMvc.perform(post("/api/v1/work-orders/1/assignment")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        
        WorkOrderAssignment current = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(1L).orElse(null);
        assertNotNull(current);
        assertEquals(SecurityTestFixtures.TECH_B_ID, current.getTechnician().getUserId());

        
        UpdateInspectionResultsRequest updateReq = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(2L)
                                .done(true)
                                .condition("Vệ sinh bằng phiên cũ")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/1/results")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WO_NOT_ASSIGNED"));

        
        WorkOrderAssignment stillCurrent = assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(1L).orElse(null);
        assertNotNull(stillCurrent);
        assertEquals(SecurityTestFixtures.TECH_B_ID, stillCurrent.getTechnician().getUserId());

        
        AuditLog log = fixtures.findLatestAuditLog("ACCESS_DENIED", "WORK_ORDER", "1")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
        assertEquals("WO_NOT_ASSIGNED", log.getReasonCode());
    }

    @Test
    @Order(4)
    @DisplayName("SEC-TC-522 TECH_A gọi accept WO_P (đã Submitted)")
    void sec_tc_522_technician_cannot_accept_submitted_wo() throws Exception {
        String techAToken = fixtures.tokenTechA();

        AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Kỹ thuật viên tự duyệt")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/3/accept")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden());

        
        MaintenanceWorkOrder wo = workOrderRepository.findById(3L).orElse(null);
        assertNotNull(wo);
        assertEquals(WorkOrderStatus.SUBMITTED, wo.getStatus());
        assertTrue(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(3L).isEmpty(), "Không được có bản ghi acceptance nào");

        
        AuditLog log = fixtures.findLatestAuditLog("ACCESS_DENIED", "ENDPOINT", "/api/v1/work-orders/3/accept")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
    }

    @Test
    @Order(5)
    @DisplayName("SEC-TC-502 Manager accept WO chưa SUBMITTED -> 409 NOT_SUBMITTED; thử đổi status trực tiếp bị từ chối")
    void sec_tc_502_cannot_close_wo_without_valid_acceptance() throws Exception {
        String mgrToken = fixtures.tokenManager1();

        
        AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Nghiệm thu sớm trái quy trình")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/2/accept")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_SUBMITTED"));

        
        MaintenanceWorkOrder wo = workOrderRepository.findById(2L).orElse(null);
        assertNotNull(wo);
        assertEquals(WorkOrderStatus.ASSIGNED, wo.getStatus());
        assertNull(wo.getClosedAt());

        
        mockMvc.perform(put("/api/v1/work-orders/2")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(6)
    @DisplayName("SEC-TC-523 Gửi cập nhật hợp lệ kèm approvedBy/status giả -> 400 Bad Request; WO không đóng và không có bản ghi acceptance")
    void sec_tc_523_tampered_request_with_fake_approved_by_or_status_rejected() throws Exception {
        String techBToken = fixtures.tokenTechB();

        
        String tamperedPayload = """
                {
                    "items": [
                        {
                            "checklistId": 1,
                            "done": true,
                            "condition": "Đạt chuẩn",
                            "defect": "Không",
                            "workDone": "Đã làm"
                        }
                    ],
                    "status": "CLOSED",
                    "approvedBy": "MGR01"
                }
                """;

        mockMvc.perform(put("/api/v1/work-orders/1/results")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));

        
        MaintenanceWorkOrder wo = workOrderRepository.findById(1L).orElse(null);
        assertNotNull(wo);
        assertNotEquals(WorkOrderStatus.CLOSED, wo.getStatus());
        assertNull(wo.getClosedAt());
        assertTrue(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(1L).isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("SEC-TC-521 MGR01 (≠ người thực hiện) nghiệm thu PASS WO_P")
    void sec_tc_521_manager_can_accept_pass_submitted_wo_not_executed_by_self() throws Exception {
        String mgrToken = fixtures.tokenManager1();

        
        AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Nghiệm thu ĐẠT toàn bộ checklist quang học")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/3/accept")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.woId").value(3))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        
        MaintenanceWorkOrder wo = workOrderRepository.findById(3L).orElse(null);
        assertNotNull(wo);
        assertEquals(WorkOrderStatus.CLOSED, wo.getStatus());
        assertNotNull(wo.getClosedAt());

        Equipment eq = equipmentRepository.findById(3L).orElse(null);
        assertNotNull(eq);
        assertEquals(EquipmentStatus.ACTIVE, eq.getStatus());

        
        MaintenancePlan plan = planRepository.findByEquipment_EquipmentId(3L).orElse(null);
        assertNotNull(plan);
        assertEquals(LocalDate.now(), plan.getLastCompletedDate());
        assertEquals(LocalDate.now().plusDays(90), plan.getNextDueDate());

        
        AuditLog log = fixtures.findLatestAuditLog("WO_ACCEPT", "WORK_ORDER", "3")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.SUCCESS, log.getOutcome());
        assertEquals(SecurityTestFixtures.MGR01_ID, log.getActorId());
        assertEquals("MANAGER", log.getActorRole());
    }

    @Test
    @Order(8)
    @DisplayName("SEC-TC-512 Đối chiếu log nghiệm thu của SEC-TC-521")
    void sec_tc_512_verify_acceptance_audit_log_actor_is_manager_not_technician() {
        
        AuditLog log = fixtures.findLatestAuditLog("WO_ACCEPT", "WORK_ORDER", "3")
                .orElse(null);
        assertNotNull(log, "Bản ghi audit log WO_ACCEPT trên WO_P phải tồn tại");

        assertEquals(SecurityTestFixtures.MGR01_ID, log.getActorId(), "Actor phải là MGR01");
        assertEquals("MANAGER", log.getActorRole());
        assertEquals("WORK_ORDER", log.getTargetType());
        assertEquals("3", log.getTargetId());
        assertEquals(AuditOutcome.SUCCESS, log.getOutcome());

        
        assertNotEquals(SecurityTestFixtures.TECH_A_ID, log.getActorId());
        assertNotEquals("TECHNICIAN", log.getActorRole());
    }

    @Test
    @Order(9)
    @DisplayName("SEC-TC-525 TECH_A sửa kết quả WO_V (đã SUBMITTED, không có acceptance) -> 409")
    void sec_tc_525_technician_cannot_modify_results_on_submitted_wo() throws Exception {
        String techAToken = fixtures.tokenTechA();

        
        UpdateInspectionResultsRequest request = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(7L)
                                .done(true)
                                .condition("Cố tình sửa đổi sau khi đã nộp")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/5/results")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_VIOLATION"));

        
        InspectionResult res = inspectionResultRepository
                .findByWorkOrder_WoIdAndChecklist_ChecklistId(5L, 7L).orElse(null);
        assertNotNull(res);
        assertNotEquals("Cố tình sửa đổi sau khi đã nộp", res.getCondition());

        
        AuditLog log = fixtures.findLatestAuditLog("STATE_VIOLATION", "ENDPOINT", "/api/v1/work-orders/5/results")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
    }

    @Test
    @Order(10)
    @DisplayName("SEC-TC-504 TECH_A sửa trực tiếp WO_C (Closed) -> 409 WO_CLOSED_IMMUTABLE")
    void sec_tc_504_technician_cannot_modify_closed_wo() throws Exception {
        String techAToken = fixtures.tokenTechA();

        
        UpdateInspectionResultsRequest request = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(4L)
                                .done(true)
                                .condition("Sửa WO đã đóng")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/4/results")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WO_CLOSED_IMMUTABLE"));

        
        MaintenanceWorkOrder wo = workOrderRepository.findById(4L).orElse(null);
        assertNotNull(wo);
        assertEquals(WorkOrderStatus.CLOSED, wo.getStatus());

        
        AuditLog log = fixtures.findLatestAuditLog("STATE_VIOLATION", "ENDPOINT", "/api/v1/work-orders/4/results")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
        assertEquals("WO_CLOSED_IMMUTABLE", log.getReasonCode());
    }

    @Test
    @Order(11)
    @DisplayName("SEC-TC-505 TECH_A gọi POST /api/v1/users -> 403 ACCESS_DENIED")
    void sec_tc_505_technician_cannot_create_user() throws Exception {
        String techAToken = fixtures.tokenTechA();

        CreateUserRequest userReq = CreateUserRequest.builder()
                .username("HACKER_USER")
                .password("Password123")
                .fullName("Hacker Account")
                .roleId(1)
                .build();

        long countBefore = userRepository.count();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        long countAfter = userRepository.count();
        assertEquals(countBefore, countAfter);
        assertTrue(userRepository.findByUsername("HACKER_USER").isEmpty());

        
        AuditLog log = fixtures.findLatestAuditLog("ACCESS_DENIED", "ENDPOINT", "/api/v1/users")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.DENIED, log.getOutcome());
    }

    @Test
    @Order(12)
    @DisplayName("SEC-TC-506 Manager xác nhận OOS cho EQ01 -> 200, log EQUIPMENT_STATUS_CHANGE")
    void sec_tc_506_manager_can_confirm_oos_status_request() throws Exception {
        String techBToken = fixtures.tokenTechB();
        String mgrToken = fixtures.tokenManager1();

        
        CreateStatusRequestDto proposeDto = CreateStatusRequestDto.builder()
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Trục quay bị kẹt nghiêm trọng, nguy cơ cháy nổ motor")
                .woId(1L)
                .build();

        mockMvc.perform(post("/api/v1/equipment/1/status-requests")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proposeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        
        Long requestId = jdbcTemplate.queryForObject(
                "SELECT request_id FROM status_change_request WHERE equipment_id = 1 AND status = 'PENDING' ORDER BY request_id DESC LIMIT 1",
                Long.class
        );
        assertNotNull(requestId);

        
        StatusRequestDecisionDto decisionDto = StatusRequestDecisionDto.builder()
                .decision("CONFIRM")
                .note("Đồng ý dừng thiết bị để thẩm định sửa chữa")
                .build();

        mockMvc.perform(post("/api/v1/status-requests/" + requestId + "/decision")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        
        Equipment eq = equipmentRepository.findById(1L).orElse(null);
        assertNotNull(eq);
        assertEquals(EquipmentStatus.OUT_OF_SERVICE, eq.getStatus());

        
        AuditLog log = fixtures.findLatestAuditLog("EQUIPMENT_STATUS_CHANGE", "EQUIPMENT", "1")
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.SUCCESS, log.getOutcome());
        assertEquals(SecurityTestFixtures.MGR01_ID, log.getActorId());
        assertEquals("MANAGER", log.getActorRole());
    }

    

    @Test
    @Order(13)
    @DisplayName("SEC-TC-508 Khóa đăng nhập sau 5 lần sai liên tiếp -> 401 INVALID_CREDENTIALS (lỗi chung chống dò quét), AuditLog ghi nhận LOCKED")
    void sec_tc_508_account_locked_after_5_failed_logins() throws Exception {
        LoginRequest badReq = new LoginRequest("USER02", "WrongPassword123!");

        
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

        
        AppUser lockedUser = userRepository.findByUsername("USER02").orElse(null);
        assertNotNull(lockedUser);
        assertTrue(lockedUser.getFailedLoginCount() >= 5);
        assertNotNull(lockedUser.getLockedUntil());

       
        AuditLog log = fixtures.findLatestAuditLog("LOGIN", "USER", String.valueOf(lockedUser.getUserId()))
                .orElse(null);
        assertNotNull(log);
        assertEquals(AuditOutcome.FAILED, log.getOutcome());
        assertEquals("LOCKED", log.getReasonCode());
    }

    @Test
    @Order(14)
    @DisplayName("SEC-TC-509 Token giả mạo chữ ký / hết hạn -> 401 INVALID_TOKEN / EXPIRED_TOKEN")
    void sec_tc_509_tampered_or_expired_jwt_rejected() throws Exception {
        
        String forgedToken = fixtures.tokenWithInvalidSignature(1L, "ADMIN01", "ADMIN");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));

        
        String expiredToken = fixtures.tokenExpired(1L, "ADMIN01", "ADMIN");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));
    }

    @Test
    @Order(15)
    @DisplayName("SEC-TC-510 Technician sửa kế hoạch bảo trì -> 403 ACCESS_DENIED")
    void sec_tc_510_technician_cannot_update_maintenance_plan() throws Exception {
        String techAToken = fixtures.tokenTechA();

        MaintenancePlanRequest planReq = MaintenancePlanRequest.builder()
                .cycleDays(90)
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(put("/api/v1/equipment/1/plan")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(16)
    @DisplayName("SEC-TC-511 SQL Injection trong query param q được bảo vệ bằng parameterized query")
    void sec_tc_511_sql_injection_in_search_query_is_safe() throws Exception {
        String mgrToken = fixtures.tokenManager1();

        
        String sqliQuery = "' OR '1'='1' --";

        mockMvc.perform(get("/api/v1/equipment")
                        .header("Authorization", "Bearer " + mgrToken)
                        .param("q", sqliQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        
        long equipCount = equipmentRepository.count();
        assertTrue(equipCount >= 10, "Bảng equipment vẫn nguyên vẹn");
    }

    @Test
    @Order(17)
    @DisplayName("SEC-TC-513 Append-only tables: từ chối PUT/PATCH/DELETE (405) và direct SQL update/delete bị Trigger chặn")
    void sec_tc_513_append_only_tables_reject_mutation() throws Exception {
        String mgrToken = fixtures.tokenManager1();

        
        mockMvc.perform(delete("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + fixtures.tokenAdmin()))
                .andExpect(status().isMethodNotAllowed());

        
        assertThrows(Exception.class, () -> {
            jdbcTemplate.update("UPDATE maintenance_history SET note = 'Tampered' WHERE history_id = 1");
        }, "Trigger phải chặn UPDATE trên maintenance_history");

        assertThrows(Exception.class, () -> {
            jdbcTemplate.update("DELETE FROM audit_log WHERE log_id = 1");
        }, "Trigger phải chặn DELETE trên audit_log");
    }

    @Test
    @Order(18)
    @DisplayName("SEC-TC-514 Đọc tài nguyên ngoài quyền -> 403 ACCESS_DENIED")
    void sec_tc_514_unauthorized_read_access_rejected() throws Exception {
        
        mockMvc.perform(get("/api/v1/work-orders/2")
                        .header("Authorization", "Bearer " + fixtures.tokenTechA()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WO_NOT_ASSIGNED"));

        
        mockMvc.perform(get("/api/v1/incidents/2")
                        .header("Authorization", "Bearer " + fixtures.tokenUser1()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INCIDENT_NOT_OWNED"));

        
        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + fixtures.tokenAdmin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(19)
    @DisplayName("SEC-TC-517 Technician PATCH role của chính mình -> 403; token có claim role cao hơn DB vẫn bị coi là role trong DB")
    void sec_tc_517_cannot_self_escalate_role() throws Exception {
        String techAToken = fixtures.tokenTechA();

        UpdateUserRequest updateReq = UpdateUserRequest.builder()
                .roleId(1) 
                .build();

        
        mockMvc.perform(patch("/api/v1/users/4")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        String forgedRoleToken = fixtures.tokenCustom(4L, "TECH_A", "ADMIN");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + forgedRoleToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateUserRequest.builder()
                                .username("FORGED_TEST")
                                .password("Demo@123456")
                                .fullName("Forged Role Test")
                                .roleId(3)
                                .build())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        AppUser techA = userRepository.findById(4L).orElse(null);
        assertNotNull(techA);
        assertEquals(3, techA.getRole().getRoleId());
    }

    @Test
    @Order(20)
    @DisplayName("SEC-TC-519 Technician gọi decision OOS -> 403; gửi status trong PUT /equipment/{id} không đổi trạng thái")
    void sec_tc_519_direct_equipment_status_mutation_forbidden() throws Exception {
        String techAToken = fixtures.tokenTechA();

        
        StatusRequestDecisionDto decisionDto = StatusRequestDecisionDto.builder()
                .decision("CONFIRM")
                .note("Technician tự duyệt OOS trái quyền")
                .build();

        mockMvc.perform(post("/api/v1/status-requests/1/decision")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        String putEquipmentWithStatus = """
                {
                    "brand": "Thermo Updated",
                    "model": "SL16R",
                    "serial": "SN-TH-16R-02",
                    "typeId": 1,
                    "locationId": 1,
                    "status": "OUT_OF_SERVICE"
                }
                """;

        mockMvc.perform(put("/api/v1/equipment/2")
                        .header("Authorization", "Bearer " + fixtures.tokenManager1())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(putEquipmentWithStatus))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));

       
        Equipment eq2 = equipmentRepository.findById(2L).orElse(null);
        assertNotNull(eq2);
        assertEquals(EquipmentStatus.ACTIVE, eq2.getStatus());
    }

    @Test
    @Order(21)
    @DisplayName("SEC-TC-526 Hai request tạo WO với 2 period khác nhau trên cùng thiết bị khi còn WO mở -> 409 WO_PERIOD_DUPLICATE")
    void sec_tc_526_two_create_wo_requests_with_different_period_rejected_with_409() throws Exception {
        String mgrToken = fixtures.tokenManager1();

        
        CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                .equipmentId(2L)
                .type(WorkOrderType.PERIODIC)
                .period("2026-Q2") 
                .build();

        mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WO_PERIOD_DUPLICATE"));
    }

    @Test
    @Order(22)
    @DisplayName("SEC-TC-527 Admin không thể tạo WO, không thể nghiệm thu, không thể quyết định OOS -> 403 ACCESS_DENIED")
    void sec_tc_527_admin_cannot_perform_business_operations() throws Exception {
        String adminToken = fixtures.tokenAdmin();

        
        CreateWorkOrderRequest createWoReq = CreateWorkOrderRequest.builder()
                .equipmentId(2L)
                .type(WorkOrderType.PERIODIC)
                .build();
        mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWoReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .build();
        mockMvc.perform(post("/api/v1/work-orders/3/accept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        StatusRequestDecisionDto decisionDto = StatusRequestDecisionDto.builder()
                .decision("CONFIRM")
                .build();
        mockMvc.perform(post("/api/v1/status-requests/1/decision")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(23)
    @DisplayName("TC-EM12 Báo sự cố thiếu thông tin bắt buộc -> 400 VALIDATION_ERROR")
    void tc_em12_incident_report_validation_requires_description() throws Exception {
        String userToken = fixtures.tokenUser1();

        
        IncidentCreateRequest badReq = IncidentCreateRequest.builder()
                .equipmentId(1L)
                .description("   ")
                .build();

        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    

    @Test
    @Order(24)
    @DisplayName("Endpoint GET /equipment/{id}/status-requests: Chỉ MANAGER được truy cập (200), các role khác bị từ chối 403")
    void testGetStatusRequestsByEquipment_RbacRestriction() throws Exception {
        
        mockMvc.perform(get("/api/v1/equipment/1/status-requests")
                        .header("Authorization", "Bearer " + fixtures.tokenManager1()))
                .andExpect(status().isOk());

        
        mockMvc.perform(get("/api/v1/equipment/1/status-requests")
                        .header("Authorization", "Bearer " + fixtures.tokenTechA()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/equipment/1/status-requests")
                        .header("Authorization", "Bearer " + fixtures.tokenUser1()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/equipment/1/status-requests")
                        .header("Authorization", "Bearer " + fixtures.tokenAdmin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Disabled("Blocked: rate limit not deployed at app level")
    @DisplayName("SEC-TC-516 Rate limit spam báo sự cố")
    void sec_tc_516_rate_limit_spam_incident_reports() {
        
    }

    @Test
    @Disabled("Blocked: not deployed")
    @DisplayName("SEC-TC-503 File Integrity Monitoring (FIM)")
    void sec_tc_503_file_integrity_monitoring() {
        
    }

    @Test
    @Disabled("Blocked: not deployed")
    @DisplayName("SEC-TC-518 Firewall & VPN Perimeter Protection")
    void sec_tc_518_firewall_vpn_perimeter_protection() {
        
    }
}
