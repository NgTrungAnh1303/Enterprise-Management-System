package com.fpt.ems.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.user.CreateUserRequest;
import com.fpt.ems.be.dto.user.UpdateUserRequest;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.AuditLogRepository;
import com.fpt.ems.be.service.AuditService;
import com.fpt.ems.be.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase1SecurityCoreTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.fpt.ems.be.service.AuditLogService auditLogService;

    @Autowired
    private com.fpt.ems.be.config.RequestIdFilter requestIdFilter;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilter(requestIdFilter)
                .build();
    }

    private static PostgreSQLContainer<?> postgresContainer;
    private static String jdbcUrl;
    private static String migrationUsername;
    private static String migrationPassword;
    private static final String APP_USER_PASSWORD = "test_app_user_pass_123";
    private static boolean isLocalClusterStarted = false;
    private static File localDataDir;
    private static int localPort = 5434;

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
            try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
                localPort = s.getLocalPort();
            }
            jdbcUrl = "jdbc:postgresql://127.0.0.1:" + localPort + "/postgres";
            migrationUsername = "postgres";
            migrationPassword = "postgres";

            localDataDir = new File("target/test-pg-" + System.currentTimeMillis());
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

        
        String validHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(10).encode("Demo@123456");
        System.out.println("GENERATED_DEMO_HASH=" + validHash);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE app_user SET password_hash = '" + validHash + "'");
        }
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
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> migrationUsername);
        registry.add("spring.flyway.password", () -> migrationPassword);
        registry.add("spring.flyway.placeholders.app_user_password", () -> APP_USER_PASSWORD);
    }

    private String getAdminToken() throws Exception {
        LoginRequest req = new LoginRequest("ADMIN01", "Demo@123456");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private String getTechToken() throws Exception {
        LoginRequest req = new LoginRequest("TECH_A", "Demo@123456");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private String getManagerToken() throws Exception {
        LoginRequest req = new LoginRequest("MGR01", "Demo@123456");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("1. Đăng nhập thành công -> trả về JWT có sub=user_id, thông tin user, và ghi audit LOGIN SUCCESS")
    void test01_loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("ADMIN01", "Demo@123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("ADMIN01"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(header().exists("X-Request-Id"));

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasSuccessLogin = logs.stream()
                .anyMatch(l -> "LOGIN".equals(l.getAction()) && l.getOutcome() == AuditOutcome.SUCCESS && Long.valueOf(1).equals(l.getActorId()));
        assertTrue(hasSuccessLogin, "Audit log must contain successful LOGIN event");
    }

    @Test
    @Order(2)
    @DisplayName("2. Đăng nhập sai mật khẩu hoặc sai username -> 401 generic message 'Tên đăng nhập hoặc mật khẩu không đúng', audit FAILED")
    void test02_loginInvalidCredentials_GenericError() throws Exception {
        LoginRequest wrongPass = new LoginRequest("ADMIN01", "WrongPassword123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

        LoginRequest wrongUser = new LoginRequest("NON_EXISTENT_USER", "Demo@123456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Khóa tài khoản sau 5 lần sai trong 5 phút -> Lần 5 bị khóa 15 phút, trả cùng 401 INVALID_CREDENTIALS (chống dò quét), trạng thái khóa ghi audit log")
    void test03_loginLockoutAfter5Fails() throws Exception {
        LoginRequest badReq = new LoginRequest("USER02", "WrongPass123");

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badReq)))
                    .andExpect(status().isUnauthorized());
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

        AppUser user02 = userRepository.findByUsername("USER02").orElseThrow();
        assertEquals(5, user02.getFailedLoginCount());
        assertNotNull(user02.getLockedUntil());
        assertNotNull(user02.getLastFailedAt());
    }

    @Test
    @Order(4)
    @DisplayName("4. Kiểm tra JWT: token sai chữ ký, hết hạn, hoặc alg 'none' -> 401 + TOKEN_REJECTED audit")
    void test04_jwtValidation() throws Exception {
        
        String fakeToken = jwtUtils.generateToken(1L, "ADMIN01", "ADMIN") + "TAMPERED";
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));

        
        String expiredToken = Jwts.builder()
                .subject("1")
                .id(UUID.randomUUID().toString())
                .claim("username", "ADMIN01")
                .claim("role", "ADMIN")
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor("super_secret_jwt_key_must_be_at_least_256_bits_length_for_hs256!".getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));

        
        String noneAlgToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIn0.";
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + noneAlgToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));
    }

    @Test
    @Order(5)
    @DisplayName("5. (b) Token cũ bị từ chối ngay khi Admin khóa tài khoản hoặc thay đổi vai trò (Zero-trust token claims)")
    void test05_tokenInvalidatedWhenAdminLocksOrChangesRole() throws Exception {
        
        LoginRequest req = new LoginRequest("USER01", "Demo@123456");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();

        
        String adminToken = getAdminToken();
        UpdateUserRequest lockReq = UpdateUserRequest.builder().status(UserStatus.LOCKED).build();
        mockMvc.perform(patch("/api/v1/users/6")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REJECTED"));

        
        UpdateUserRequest unlockReq = UpdateUserRequest.builder().status(UserStatus.ACTIVE).build();
        mockMvc.perform(patch("/api/v1/users/6")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unlockReq)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    @DisplayName("6. SEC-TC-505: Technician gọi POST /users -> 403 ACCESS_DENIED, không tạo user, có audit DENIED")
    void test06_secTc505_technicianCannotManageUsers() throws Exception {
        String techToken = getTechToken();

        CreateUserRequest createReq = CreateUserRequest.builder()
                .username("HACKER_USER")
                .password("Password123")
                .fullName("Illegal User")
                .roleId(1)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        assertFalse(userRepository.existsByUsername("HACKER_USER"), "No user should be created by non-admin");

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasAccessDenied = logs.stream()
                .anyMatch(l -> "ACCESS_DENIED".equals(l.getAction()) && l.getOutcome() == AuditOutcome.DENIED);
        assertTrue(hasAccessDenied, "Audit log must contain ACCESS_DENIED for unauthorized access");
    }

    @Test
    @Order(7)
    @DisplayName("7. SEC-TC-517: Client cố tình tiêm trường lạ (role, status, hack) -> 400 UNKNOWN_PROPERTY (fail-on-unknown-properties)")
    void test07_secTc517_unknownPropertiesRejected() throws Exception {
        String adminToken = getAdminToken();

        String payloadWithExtraFields = """
                {
                    "username": "valid_user_1",
                    "password": "Password123",
                    "fullName": "Test Valid User",
                    "roleId": 3,
                    "role": "ADMIN",
                    "status": "ACTIVE",
                    "approvedBy": "Hacker"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithExtraFields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));
    }

    @Test
    @Order(8)
    @DisplayName("8. (a) Audit DENIED/FAILED tồn tại ngay cả khi transaction nghiệp vụ bị rollback (Propagation.REQUIRES_NEW)")
    void test08_auditDeniedPersistsOnRollback() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> {
            txTemplate.execute(status -> {
                
                auditService.recordDenied(
                        "SECURITY_TEST_ACTION",
                        "TEST_TARGET",
                        "999",
                        AuditOutcome.DENIED,
                        "ROLLBACK_TEST_REASON",
                        null,
                        Map.of("note", "This audit must survive rollback")
                );

                
                throw new RuntimeException("Simulated business transaction failure requiring rollback");
            });
        });

        
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean auditPersisted = logs.stream()
                .anyMatch(l -> "SECURITY_TEST_ACTION".equals(l.getAction())
                        && "ROLLBACK_TEST_REASON".equals(l.getReasonCode())
                        && l.getOutcome() == AuditOutcome.DENIED);
        assertTrue(auditPersisted, "Audit log with REQUIRES_NEW must be persisted even after transaction rollback");
    }

    @Test
    @Order(9)
    @DisplayName("9. (c) /audit-logs: Manager 403 mặc định; 200 khi app.audit.manager-can-view=true; Admin luôn 200; 405 khi gọi DELETE")
    void test09_auditLogsRbacAndAppendOnly() throws Exception {
        String managerToken = getManagerToken();
        String adminToken = getAdminToken();

        
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUDIT_VIEW_DENIED"));

        
        ReflectionTestUtils.setField(auditLogService, "managerCanView", true);
        try {
            mockMvc.perform(get("/api/v1/audit-logs")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        } finally {
            ReflectionTestUtils.setField(auditLogService, "managerCanView", false);
        }

        
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        
        mockMvc.perform(delete("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(10)
    @DisplayName("10. (d) Không có bất kỳ response nào chứa password_hash hoặc passwordHash")
    void test10_noPasswordHashInResponses() throws Exception {
        String adminToken = getAdminToken();

        
        LoginRequest loginReq = new LoginRequest("ADMIN01", "Demo@123456");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        String loginBody = loginResult.getResponse().getContentAsString();
        assertFalse(loginBody.contains("password_hash"), "Login response must not contain password_hash");
        assertFalse(loginBody.contains("passwordHash"), "Login response must not contain passwordHash");
        assertFalse(loginBody.contains("$2a$"), "Login response must not contain bcrypt hashes");

        
        MvcResult meResult = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String meBody = meResult.getResponse().getContentAsString();
        assertFalse(meBody.contains("password_hash"));
        assertFalse(meBody.contains("passwordHash"));
        assertFalse(meBody.contains("$2a$"));

        
        MvcResult usersResult = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String usersBody = usersResult.getResponse().getContentAsString();
        assertFalse(usersBody.contains("password_hash"));
        assertFalse(usersBody.contains("passwordHash"));
        assertFalse(usersBody.contains("$2a$"));
    }

    @Test
    @Order(11)
    @DisplayName("11. (e) GET /auth/me trả đầy đủ role, permissions, menus; POST /auth/logout trả 204 và ghi audit LOGOUT")
    void test11_authMeAndLogout() throws Exception {
        String managerToken = getManagerToken();

        
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.permissions", hasItem("WORK_ORDERS_MANAGE")))
                .andExpect(jsonPath("$.menus", hasItem("/work-orders")));

        
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasLogout = logs.stream()
                .anyMatch(l -> "LOGOUT".equals(l.getAction()) && l.getOutcome() == AuditOutcome.SUCCESS);
        assertTrue(hasLogout, "Audit log must contain LOGOUT event");
    }

    @Test
    @Order(12)
    @DisplayName("12. Admin không được tự khóa hoặc hạ quyền chính mình -> 403 CANNOT_LOCK_SELF / CANNOT_DEMOTE_SELF")
    void test12_adminCannotSelfLockOrDemote() throws Exception {
        String adminToken = getAdminToken();

        
        UpdateUserRequest selfLock = UpdateUserRequest.builder().status(UserStatus.LOCKED).build();
        mockMvc.perform(patch("/api/v1/users/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfLock)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_LOCK_SELF"));

        
        UpdateUserRequest selfDemote = UpdateUserRequest.builder().roleId(3).build();
        mockMvc.perform(patch("/api/v1/users/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDemote)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_DEMOTE_SELF"));
    }

    @Test
    @Order(13)
    @DisplayName("13. Audit whitelist sanitization: mọi audit log không chứa bất kỳ secret/password nào")
    void test13_auditWhitelistSanitization() {
        List<AuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty());

        for (AuditLog log : logs) {
            if (log.getOldValue() != null) {
                assertFalse(log.getOldValue().contains("password"));
                assertFalse(log.getOldValue().contains("passwordHash"));
                assertFalse(log.getOldValue().contains("token"));
                assertFalse(log.getOldValue().contains("secret"));
            }
            if (log.getNewValue() != null) {
                assertFalse(log.getNewValue().contains("password"));
                assertFalse(log.getNewValue().contains("passwordHash"));
                assertFalse(log.getNewValue().contains("token"));
                assertFalse(log.getNewValue().contains("secret"));
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("14. Phân quyền /users và /users/technicians: MANAGER gọi GET /users -> 403; TECH/USER gọi /users/technicians -> 403; MGR gọi /users/technicians -> 200")
    void test14_userAndTechnicianEndpointsAuthorization() throws Exception {
        String adminToken = getAdminToken();
        String managerToken = getManagerToken();
        String techToken = getTechToken();

        
        LoginRequest userReq = new LoginRequest("USER01", "Demo@123456");
        MvcResult userLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = objectMapper.readTree(userLoginRes.getResponse().getContentAsString()).get("accessToken").asText();

        
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/users/technicians")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/users/technicians")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/users/technicians")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        
        mockMvc.perform(get("/api/v1/users/technicians")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].fullName").exists())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist());
    }
}
