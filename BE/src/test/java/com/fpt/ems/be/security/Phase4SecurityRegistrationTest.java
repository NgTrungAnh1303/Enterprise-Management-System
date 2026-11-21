package com.fpt.ems.be.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.config.RequestIdFilter;
import com.fpt.ems.be.dto.auth.LoginRequest;
import com.fpt.ems.be.dto.auth.RegisterRequest;
import com.fpt.ems.be.fixture.SecurityTestFixtures;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.AuditLogRepository;
import com.fpt.ems.be.util.IpRateLimiter;
import com.fpt.ems.be.util.JwtUtils;
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
import java.sql.Connection;
import java.sql.DriverManager;
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
public class Phase4SecurityRegistrationTest {

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
    private AppUserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private IpRateLimiter ipRateLimiter;

    @Autowired
    private RequestIdFilter requestIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilter(requestIdFilter)
                .build();
    }

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

            localDataDir = new File("target/test-pg-phase4-" + System.currentTimeMillis());
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
                    Thread.sleep(300);
                }
            }
            assertTrue(connected, "Could not connect to local PostgreSQL cluster");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, migrationUsername, migrationPassword)
                .locations("classpath:db/migration", "classpath:db/seed-dev")
                .placeholders(Map.of("app_user_password", APP_USER_PASSWORD))
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
        if (isLocalClusterStarted && localDataDir != null) {
            String psqlBinDir = "C:\\Program Files\\PostgreSQL\\16\\bin";
            new ProcessBuilder(
                    psqlBinDir + "\\pg_ctl.exe",
                    "-D", localDataDir.getAbsolutePath(),
                    "-m", "immediate",
                    "stop"
            ).inheritIO().start().waitFor(10, TimeUnit.SECONDS);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> migrationUsername);
        registry.add("spring.datasource.password", () -> migrationPassword);
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> migrationUsername);
        registry.add("spring.flyway.password", () -> migrationPassword);
        registry.add("spring.flyway.placeholders.app_user_password", () -> APP_USER_PASSWORD);
        registry.add("app.jwt.secret", () -> "secure_test_jwt_secret_key_minimum_256_bits_for_testing_only!");
        registry.add("app.registration.enabled", () -> true);
        registry.add("app.registration.require-approval", () -> true);
        registry.add("app.registration.rate-limit-per-hour", () -> 5);
    }

    private String getAdminToken() {
        return jwtUtils.generateToken(1L, "ADMIN01", "ADMIN");
    }

    private String getManagerToken() {
        return jwtUtils.generateToken(2L, "MGR01", "MANAGER");
    }

    private String getTechToken() {
        return jwtUtils.generateToken(4L, "TECH_A", "TECHNICIAN");
    }

    private String getUserToken() {
        return jwtUtils.generateToken(6L, "USER01", "EQUIPMENT_USER");
    }

    @Test
    @Order(1)
    @DisplayName("SEC-TC-530: Chống Mass Assignment - Gửi trường role/status/id lạ trong body bị 400 Bad Request")
    void secTc530_massAssignmentBlocked() throws Exception {
        ipRateLimiter.clearAll();

        String payloadWithRole = """
                {
                    "fullName": "Hacker User",
                    "email": "hacker1@example.com",
                    "password": "Password123@Admin",
                    "confirmPassword": "Password123@Admin",
                    "role": "ADMIN",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithRole))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));
    }

    @Test
    @Order(2)
    @DisplayName("SEC-TC-532: Mật khẩu yếu (<12 ký tự, thiếu hoa/thường/số, hoặc chứa họ tên/email) bị 400 Bad Request")
    void secTc532_weakPasswordRejected() throws Exception {
        ipRateLimiter.clearAll();

        
        RegisterRequest shortPwd = new RegisterRequest("Nguyen Van A", "user_short@example.com", "Abc12345", "Abc12345");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPwd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        
        RegisterRequest noDigit = new RegisterRequest("Nguyen Van A", "user_nodigit@example.com", "PasswordWithoutDigit", "PasswordWithoutDigit");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDigit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        
        RegisterRequest containsEmail = new RegisterRequest("Nguyen Van A", "tester@example.com", "Tester12345678", "Tester12345678");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(containsEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        
        RegisterRequest mismatch = new RegisterRequest("Nguyen Van A", "mismatch@example.com", "ValidPass123456", "DifferentPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatch)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
    }

    @Test
    @Order(3)
    @DisplayName("SEC-TC-533: Giới hạn tần suất đăng ký (IP Rate Limiting) - Lần thứ 6 trong 1 giờ từ cùng IP nhận 429")
    void secTc533_rateLimiting() throws Exception {
        ipRateLimiter.clearAll();
        String testIp = "192.168.1.100";

        
        for (int i = 1; i <= 5; i++) {
            RegisterRequest req = new RegisterRequest(
                    "User Rate " + i,
                    "ratetest" + i + "@example.com",
                    "StrongPass123456!",
                    "StrongPass123456!"
            );
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        
        RegisterRequest req6 = new RegisterRequest(
                "User Rate 6",
                "ratetest6@example.com",
                "StrongPass123456!",
                "StrongPass123456!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> {
                            request.setRemoteAddr(testIp);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req6)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));

        
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasRateLimitLog = logs.stream()
                .anyMatch(l -> "REGISTER".equals(l.getAction()) && "REGISTER_RATE_LIMITED".equals(l.getReasonCode()));
        assertTrue(hasRateLimitLog, "Phải ghi nhận audit log REGISTER_RATE_LIMITED");
    }

    @Test
    @Order(4)
    @DisplayName("SEC-TC-531: Chống dò quét Email - Đăng ký email trùng trả về cùng mã 200, cùng message và không tạo bản ghi mới")
    void secTc531_antiEmailEnumeration() throws Exception {
        ipRateLimiter.clearAll();

        String email = "unique_register_user@example.com";
        RegisterRequest req = new RegisterRequest(
                "Le Van Dang Ky",
                email,
                "StrongSecurePass123!",
                "StrongSecurePass123!"
        );

        
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requireApproval").value(true))
                .andReturn();

        String body1 = res1.getResponse().getContentAsString();

        
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requireApproval").value(true))
                .andReturn();

        String body2 = res2.getResponse().getContentAsString();

        
        assertEquals(body1, body2, "Phản hồi đăng ký trùng email phải giống hệt đăng ký thành công");

        
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasDuplicateEmailLog = logs.stream()
                .anyMatch(l -> "REGISTER".equals(l.getAction()) && "REGISTER_DUPLICATE_EMAIL".equals(l.getReasonCode()));
        assertTrue(hasDuplicateEmailLog, "Phải ghi nhận audit log REGISTER_DUPLICATE_EMAIL");
    }

    @Test
    @Order(5)
    @DisplayName("SEC-TC-534: Tài khoản PENDING_APPROVAL đăng nhập bị từ chối với 401 generic 'Tên đăng nhập hoặc mật khẩu không đúng'")
    void secTc534_pendingApprovalLoginRejectedGeneric() throws Exception {
        
        LoginRequest loginReq = new LoginRequest("unique_register_user@example.com", "StrongSecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    @Order(6)
    @DisplayName("SEC-TC-538: Quản trị viên (ADMIN) duyệt tài khoản PENDING_APPROVAL -> ACTIVE, các role khác bị 403")
    void secTc538_adminApprovalAndRbac() throws Exception {
        AppUser pendingUser = userRepository.findByEmailIgnoreCase("unique_register_user@example.com").orElseThrow();
        assertEquals(UserStatus.PENDING_APPROVAL, pendingUser.getStatus());

        
        mockMvc.perform(post("/api/v1/users/" + pendingUser.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getTechToken()))
                .andExpect(status().isForbidden());

        
        mockMvc.perform(post("/api/v1/users/" + pendingUser.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getManagerToken()))
                .andExpect(status().isForbidden());

        
        mockMvc.perform(post("/api/v1/users/" + pendingUser.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        AppUser approvedUser = userRepository.findById(pendingUser.getUserId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, approvedUser.getStatus());

        
        LoginRequest loginReq = new LoginRequest("unique_register_user@example.com", "StrongSecurePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value(RoleName.EQUIPMENT_USER.name()));
    }

    @Test
    @Order(7)
    @DisplayName("SEC-TC-538B: Từ chối tài khoản PENDING_APPROVAL -> REJECTED và không cho phép đăng nhập")
    void secTc538b_adminRejectUser() throws Exception {
        ipRateLimiter.clearAll();

        String email = "rejected_user@example.com";
        RegisterRequest req = new RegisterRequest(
                "Nguoi Bi Tu Choi",
                email,
                "StrongPass123456!",
                "StrongPass123456!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        
        mockMvc.perform(post("/api/v1/users/" + user.getUserId() + "/reject")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        
        LoginRequest loginReq = new LoginRequest(email, "StrongPass123456!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requireApproval").value(true));

        AppUser rechecked = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(UserStatus.REJECTED, rechecked.getStatus(), "Trạng thái vẫn phải là REJECTED, không tạo mới");
    }

    @Test
    @Order(8)
    @DisplayName("Điều kiện 2: Chống state violation - Duyệt/từ chối tài khoản không phải PENDING_APPROVAL trả về 409 STATE_VIOLATION")
    void secTc_stateViolation() throws Exception {
        
        AppUser adminUser = userRepository.findByUsername("ADMIN01").orElseThrow();

        mockMvc.perform(post("/api/v1/users/" + adminUser.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_VIOLATION"));

        mockMvc.perform(post("/api/v1/users/" + adminUser.getUserId() + "/reject")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_VIOLATION"));
    }

    @Test
    @Order(9)
    @DisplayName("SEC-TC-538B: GET /api/v1/public/overview không có JWT trả về 401 Unauthorized (Deny-by-default)")
    void secTc538b_publicOverviewDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/public/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("SEC-TC-538B (Scan): Toàn bộ các endpoint nghiệp vụ khác đều trả 401 khi không có JWT")
    void secTc536_denyByDefaultScan() throws Exception {
        
        mockMvc.perform(get("/api/v1/public/overview"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(get("/api/v1/work-orders"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized());

        
        mockMvc.perform(get("/api/v1/equipment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("Điều kiện 1: Đăng nhập tra username trước rồi email, tính đúng lockout khi sai liên tiếp -> trả 401 lỗi chung")
    void secTc_usernameThenEmailAndLockout() throws Exception {
        
        ipRateLimiter.clearAll();
        String email = "lockout_test@example.com";
        RegisterRequest regReq = new RegisterRequest("Lockout Test", email, "MyStrongPassword123!", "MyStrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        
        mockMvc.perform(post("/api/v1/users/" + user.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk());

        
        LoginRequest badEmailReq = new LoginRequest(email, "WrongPassword123");
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badEmailReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEmailReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

        
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEmailReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

        
        AppUser lockedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(5, lockedUser.getFailedLoginCount());
        assertNotNull(lockedUser.getLockedUntil());
    }

    @Test
    @Order(12)
    @DisplayName("Bảo mật: Tài khoản trạng thái LOCKED bởi Quản trị viên đăng nhập trả lỗi chung 401, không trả 403")
    void secTc_administrativeLockedAccountReturnsGeneric401() throws Exception {
        ipRateLimiter.clearAll();
        String email = "admin_locked_user@example.com";
        RegisterRequest regReq = new RegisterRequest("Admin Locked User", email, "MyStrongPassword123!", "MyStrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);

        LoginRequest loginReq = new LoginRequest(email, "MyStrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));

       
        AuditLog audit = auditLogRepository.findAll().stream()
                .filter(a -> "LOGIN".equals(a.getAction()) && String.valueOf(user.getUserId()).equals(a.getTargetId()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(AuditOutcome.FAILED, audit.getOutcome());
        assertEquals("LOCKED", audit.getReasonCode());
    }

    @Test
    @Order(13)
    @DisplayName("Validation: Đăng ký với fullName hoặc email vượt quá 100 ký tự trả về 400 Bad Request")
    void secTc_registerValidationFieldLength() throws Exception {
        ipRateLimiter.clearAll();
        
        String longName = "A".repeat(101);
        RegisterRequest reqLongName = new RegisterRequest(longName, "valid_email@example.com", "MyStrongPassword123!", "MyStrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLongName)))
                .andExpect(status().isBadRequest());

        
        String longEmail = "b".repeat(95) + "@example.com"; 
        RegisterRequest reqLongEmail = new RegisterRequest("Valid Name", longEmail, "MyStrongPassword123!", "MyStrongPassword123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLongEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    @DisplayName("Luồng người dùng: EQUIPMENT_USER mới duyệt xem danh sách sự cố rỗng ban đầu, sau đó chỉ xem được sự cố của mình")
    void secTc_approvedUserIncidentIsolation() throws Exception {
        ipRateLimiter.clearAll();
        String email = "incident_user@example.com";
        String password = "MyStrongPassword123!";
        RegisterRequest regReq = new RegisterRequest("Incident Test User", email, password, password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk());

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        
        mockMvc.perform(post("/api/v1/users/" + user.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + getAdminToken()))
                .andExpect(status().isOk());

        
        LoginRequest loginReq = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));

        
        String createIncidentJson = """
                {
                    "equipmentId": 1,
                    "description": "Sự cố kiểm thử cách ly dữ liệu người dùng mới"
                }
                """;
        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createIncidentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Sự cố kiểm thử cách ly dữ liệu người dùng mới"));

        
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Sự cố kiểm thử cách ly dữ liệu người dùng mới"));
    }
}
