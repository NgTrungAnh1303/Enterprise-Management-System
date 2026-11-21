package com.fpt.ems.be;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.incident.ClassifyIncidentRequest;
import com.fpt.ems.be.dto.incident.IncidentCreateRequest;
import com.fpt.ems.be.dto.incident.IncidentProgressRequest;
import com.fpt.ems.be.dto.statusrequest.CreateStatusRequestDto;
import com.fpt.ems.be.dto.statusrequest.StatusRequestDecisionDto;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.model.enums.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase2BIntegrationFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static PostgreSQLContainer<?> postgresContainer;
    private static String jdbcUrl;
    private static String migrationUsername;
    private static String migrationPassword;
    private static final String APP_USER_PASSWORD = "test_app_user_pass_123";
    private static boolean isLocalClusterStarted = false;
    private static File localDataDir;
    private static int localPort;

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

            localDataDir = new File("target/test-pg-phase2b-" + System.currentTimeMillis());
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
                .build();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

 

    @Test
    @Order(1)
    @DisplayName("Negative: 403 khi non-Manager truy cập /dashboard và /alerts")
    void testNegative_DashboardAndAlerts_NonManagerForbidden() throws Exception {
        String techToken = loginAndGetToken("TECH_A", "Demo@123456");
        String userToken = loginAndGetToken("USER01", "Demo@123456");
        String adminToken = loginAndGetToken("ADMIN01", "Demo@123456");

         mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

         mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    @DisplayName("Negative: 405 Method Not Allowed khi gửi POST, PUT, DELETE trên /equipment/{id}/history (AZ-11)")
    void testNegative_HistoryEndpoint_MethodNotAllowedOnMutations() throws Exception {
        String mgrToken = loginAndGetToken("MGR01", "Demo@123456");

        mockMvc.perform(post("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(put("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(delete("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @Order(3)
    @DisplayName("Negative: 409 nghiệm thu WO chưa SUBMITTED; 400 FAIL thiếu note; 409 điều chỉnh WO chưa CLOSED")
    void testNegative_WorkOrderAcceptanceAndAdjustmentErrors() throws Exception {
        String mgrToken = loginAndGetToken("MGR01", "Demo@123456");

         AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .build();
        mockMvc.perform(post("/api/v1/work-orders/1/accept")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_SUBMITTED"));

         AcceptWorkOrderRequest failReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.FAIL)
                .note("   ")
                .build();
        mockMvc.perform(post("/api/v1/work-orders/3/accept")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_REQUIRED"));

         CreateAdjustmentRequest adjReq = CreateAdjustmentRequest.builder()
                .reason("Điều chỉnh phụ tùng")
                .build();
        mockMvc.perform(post("/api/v1/work-orders/1/adjustments")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_VIOLATION"));
    }

    @Test
    @Order(4)
    @DisplayName("Negative: Quyền đọc sự cố (AZ-07) và lịch sử thiết bị (AZ-12)")
    void testNegative_ReadPermissions_Az07AndAz12() throws Exception {
        String user1Token = loginAndGetToken("USER01", "Demo@123456");
        String user2Token = loginAndGetToken("USER02", "Demo@123456");
        String techBToken = loginAndGetToken("TECH_B", "Demo@123456");
        String adminToken = loginAndGetToken("ADMIN01", "Demo@123456");

         IncidentCreateRequest incReq = IncidentCreateRequest.builder()
                .equipmentId(1L)
                .description("Màn hình không lên nguồn")
                .build();
        MvcResult incRes = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long incId = objectMapper.readTree(incRes.getResponse().getContentAsString()).get("incidentId").asLong();

         mockMvc.perform(get("/api/v1/incidents/" + incId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INCIDENT_NOT_OWNED"));

         mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

         mockMvc.perform(get("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + techBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

 

    @Test
    @Order(5)
    @DisplayName("QT-01: Luồng bảo trì định kỳ hoàn chỉnh từ tạo WO, phân công, thực hiện, nộp, nghiệm thu PASS, đóng và điều chỉnh")
    void testQT01_PeriodicMaintenanceEndToEndFlow() throws Exception {
        String mgr1Token = loginAndGetToken("MGR01", "Demo@123456");
        String mgr2Token = loginAndGetToken("MGR02", "Demo@123456");
        String techAToken = loginAndGetToken("TECH_A", "Demo@123456");
        String techBToken = loginAndGetToken("TECH_B", "Demo@123456");
        String adminToken = loginAndGetToken("ADMIN01", "Demo@123456");
        String userToken = loginAndGetToken("USER01", "Demo@123456");

         CreateWorkOrderRequest createWoReq = CreateWorkOrderRequest.builder()
                .equipmentId(5L)
                .type(WorkOrderType.PERIODIC)
                .build();

        MvcResult createWoRes = mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.period").isNotEmpty())
                .andReturn();

        Long woId = objectMapper.readTree(createWoRes.getResponse().getContentAsString()).get("woId").asLong();

         AssignWorkOrderRequest assignReq = AssignWorkOrderRequest.builder()
                .technicianId(4L)
                .build();

        mockMvc.perform(post("/api/v1/work-orders/" + woId + "/assignment")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.currentAssignment.technicianId").value(4));

         mockMvc.perform(post("/api/v1/work-orders/" + woId + "/start")
                        .header("Authorization", "Bearer " + techAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.equipment.status").value("UNDER_MAINTENANCE"));

       
        UpdateInspectionResultsRequest resultsReq = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(7L)
                                .done(true)
                                .condition("Nguồn đèn hoạt động tốt")
                                .defect("Không có")
                                .workDone("Đo quang thông và kiểm tra nguồn")
                                .build(),
                        InspectionResultItemDto.builder()
                                .checklistId(8L)
                                .done(true)
                                .condition("Bước sóng chuẩn")
                                .defect("Không có")
                                .workDone("Hiệu chuẩn bằng kính Holmium")
                                .build(),
                        InspectionResultItemDto.builder()
                                .checklistId(9L)
                                .done(true)
                                .condition("Buồng đo sạch")
                                .defect("Không có")
                                .workDone("Vệ sinh bằng giấy lau quang học")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/" + woId + "/results")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resultsReq)))
                .andExpect(status().isOk());

         mockMvc.perform(post("/api/v1/work-orders/" + woId + "/submit")
                        .header("Authorization", "Bearer " + techAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

         AcceptWorkOrderRequest acceptReq = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Bảo trì định kỳ đạt tiêu chuẩn phòng thí nghiệm")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/" + woId + "/accept")
                        .header("Authorization", "Bearer " + mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.equipment.status").value("ACTIVE"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

         CreateAdjustmentRequest adjReq = CreateAdjustmentRequest.builder()
                .reason("Cập nhật số giờ chạy bóng đèn quang phổ")
                .changeDetail("{\"operatingHours\": 1250}")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + mgr2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentId").isNotEmpty())
                .andExpect(jsonPath("$.reason").value("Cập nhật số giờ chạy bóng đèn quang phổ"));

        
        mockMvc.perform(get("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + mgr2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("Cập nhật số giờ chạy bóng đèn quang phổ"));

        
        mockMvc.perform(get("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + techAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

         mockMvc.perform(get("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + techBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WO_NOT_ASSIGNED"));

         mockMvc.perform(get("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

         mockMvc.perform(get("/api/v1/work-orders/" + woId + "/adjustments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

         mockMvc.perform(put("/api/v1/work-orders/" + woId + "/results")
                        .header("Authorization", "Bearer " + techAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resultsReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WO_CLOSED_IMMUTABLE"));
    }

     

    @Test
    @Order(6)
    @DisplayName("QT-02: Luồng sự cố & sửa chữa hoàn chỉnh: Báo sự cố -> Phân loại -> Tạo WO sửa chữa -> Phân công (incident->ASSIGNED) -> Đề xuất OOS (PENDING, thiết bị chưa đổi) -> Manager xác nhận OOS -> Sửa chữa -> RESOLVED -> Nghiệm thu PASS -> Đóng sự cố")
    void testQT02_IncidentRepairEndToEndFlow() throws Exception {
        String user1Token = loginAndGetToken("USER01", "Demo@123456");
        String mgr1Token = loginAndGetToken("MGR01", "Demo@123456");
        String techBToken = loginAndGetToken("TECH_B", "Demo@123456");

         IncidentCreateRequest incReq = IncidentCreateRequest.builder()
                .equipmentId(7L)
                .description("Máy phát ra mùi khét khi vận hành ở tốc độ cao")
                .build();

        MvcResult incRes = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.trackingCode").value(startsWith("INC-")))
                .andReturn();

        Long incidentId = objectMapper.readTree(incRes.getResponse().getContentAsString()).get("incidentId").asLong();

         ClassifyIncidentRequest classReq = ClassifyIncidentRequest.builder()
                .severity(IncidentSeverity.HIGH)
                .build();

        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/classify")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLASSIFIED"))
                .andExpect(jsonPath("$.severity").value("HIGH"));

         CreateWorkOrderRequest repairWoReq = CreateWorkOrderRequest.builder()
                .equipmentId(7L)
                .type(WorkOrderType.REPAIR)
                .incidentId(incidentId)
                .build();

        MvcResult repairWoRes = mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairWoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andReturn();

        Long repairWoId = objectMapper.readTree(repairWoRes.getResponse().getContentAsString()).get("woId").asLong();

        
        AssignWorkOrderRequest assignReq = AssignWorkOrderRequest.builder()
                .technicianId(5L)
                .build();

        mockMvc.perform(post("/api/v1/work-orders/" + repairWoId + "/assignment")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

         mockMvc.perform(get("/api/v1/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

         CreateStatusRequestDto oosReq = CreateStatusRequestDto.builder()
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Cần tháo động cơ quấn lại cuộn dây, dừng máy an toàn")
                .woId(repairWoId)
                .build();

        MvcResult oosRes = mockMvc.perform(post("/api/v1/equipment/7/status-requests")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oosReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long reqId = objectMapper.readTree(oosRes.getResponse().getContentAsString()).get("requestId").asLong();

         mockMvc.perform(get("/api/v1/equipment/7")
                        .header("Authorization", "Bearer " + mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

         mockMvc.perform(post("/api/v1/equipment/7/status-requests")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oosReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_REQUEST_EXISTS"));

        StatusRequestDecisionDto oosDec = StatusRequestDecisionDto.builder()
                .decision("CONFIRM")
                .note("Đồng ý ngưng máy theo đề xuất của KTV")
                .build();

        mockMvc.perform(post("/api/v1/status-requests/" + reqId + "/decision")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oosDec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/equipment/7")
                        .header("Authorization", "Bearer " + mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        mockMvc.perform(post("/api/v1/work-orders/" + repairWoId + "/start")
                        .header("Authorization", "Bearer " + techBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.equipment.status").value("OUT_OF_SERVICE"));

       
        UpdateInspectionResultsRequest resReq = UpdateInspectionResultsRequest.builder()
                .items(List.of(
                        InspectionResultItemDto.builder()
                                .checklistId(1L)
                                .done(true)
                                .condition("Đã thay bạc đạn mới")
                                .defect("Bạc đạn cũ rơ")
                                .workDone("Tháo lắp bạc đạn NSK mới")
                                .build(),
                        InspectionResultItemDto.builder()
                                .checklistId(2L)
                                .done(true)
                                .condition("Vệ sinh khoang sạch")
                                .defect("Dính dầu mỡ cũ")
                                .workDone("Tẩy rửa khoang và bôi trơn ron cao su")
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/work-orders/" + repairWoId + "/results")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/work-orders/" + repairWoId + "/submit")
                        .header("Authorization", "Bearer " + techBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

         IncidentProgressRequest progReq = IncidentProgressRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .note("Đã quấn lại động cơ và thay bạc đạn, thiết bị chạy êm")
                .build();

        mockMvc.perform(patch("/api/v1/incidents/" + incidentId + "/progress")
                        .header("Authorization", "Bearer " + techBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

         AcceptWorkOrderRequest repairAccept = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Nghiệm thu chạy thử đạt chuẩn tốc độ tối đa")
                .build();

        mockMvc.perform(post("/api/v1/work-orders/" + repairWoId + "/accept")
                        .header("Authorization", "Bearer " + mgr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairAccept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.equipment.status").value("ACTIVE"));

         mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/close")
                        .header("Authorization", "Bearer " + mgr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }
}

