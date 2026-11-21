package com.fpt.ems.be.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.config.RequestIdFilter;
import com.fpt.ems.be.dto.workorder.CreateWorkOrderRequest;
import com.fpt.ems.be.fixture.SecurityTestFixtures;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import com.fpt.ems.be.repository.MaintenanceWorkOrderRepository;
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
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase3PerformanceAndConcurrencyTest {

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
    private RequestIdFilter requestIdFilter;

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

            localDataDir = new File("target/test-pg-perf-" + System.currentTimeMillis());
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
    }

    // =========================================================================
    // TASK: PERFORMANCE TEST (5,000 EQUIPMENTS + 20,000 HISTORIES < 2s)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("NFR-04 Performance Test: Seed 5.000 thiết bị & 20.000 history; kiểm các API phân trang < 2 giây")
    void testPerformance_LargeDatasetResponseTimeUnder2Seconds() throws Exception {
        String mgrToken = fixtures.tokenManager1();
        String adminToken = fixtures.tokenAdmin();

        // 1. Nạp 5.000 thiết bị + 20.000 history bằng migration_user
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
            conn.setAutoCommit(false);

            // Seed 5.000 equipments
            String insertEquipSql = "INSERT INTO equipment (code, brand, model, serial, status, type_id, location_id) " +
                    "VALUES (?, 'BrandTest', 'ModelTest', ?, 'ACTIVE', 1, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertEquipSql)) {
                for (int i = 1; i <= 5000; i++) {
                    ps.setString(1, "PERF_EQ_" + i);
                    ps.setString(2, "SN_PERF_" + i);
                    ps.addBatch();
                    if (i % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }

            // Seed 20.000 histories cho EQ01 (id 1)
            String insertHistSql = "INSERT INTO maintenance_history (equipment_id, action, old_status, new_status, performed_by, note) " +
                    "VALUES (1, 'PERIODIC_MAINTENANCE', 'UNDER_MAINTENANCE', 'ACTIVE', 2, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertHistSql)) {
                for (int i = 1; i <= 20000; i++) {
                    ps.setString(1, "Bảo trì định kỳ hiệu năng số " + i);
                    ps.addBatch();
                    if (i % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }

            conn.commit();
        }

        // 2. Đo thời gian phản hồi: GET /api/v1/equipment?page=0&size=20
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/equipment")
                        .header("Authorization", "Bearer " + mgrToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        long equipDuration = System.currentTimeMillis() - start;
        assertTrue(equipDuration < 2000, "GET /equipment phải < 2000ms (thực tế: " + equipDuration + "ms)");

        // 3. Đo thời gian phản hồi: GET /api/v1/equipment/1/history?page=0&size=20 (trên 20.000 bản ghi)
        start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/equipment/1/history")
                        .header("Authorization", "Bearer " + mgrToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        long historyDuration = System.currentTimeMillis() - start;
        assertTrue(historyDuration < 2000, "GET /equipment/1/history phải < 2000ms (thực tế: " + historyDuration + "ms)");

        // 4. Đo thời gian phản hồi: GET /api/v1/work-orders?page=0&size=20
        start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + mgrToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        long woDuration = System.currentTimeMillis() - start;
        assertTrue(woDuration < 2000, "GET /work-orders phải < 2000ms (thực tế: " + woDuration + "ms)");

        // 5. Đo thời gian phản hồi: GET /api/v1/incidents?page=0&size=20
        start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + mgrToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        long incidentDuration = System.currentTimeMillis() - start;
        assertTrue(incidentDuration < 2000, "GET /incidents phải < 2000ms (thực tế: " + incidentDuration + "ms)");

        // 6. Đo thời gian phản hồi: GET /api/v1/audit-logs?page=0&size=20
        start = System.currentTimeMillis();
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        long auditDuration = System.currentTimeMillis() - start;
        assertTrue(auditDuration < 2000, "GET /audit-logs phải < 2000ms (thực tế: " + auditDuration + "ms)");

        // 7. Dọn dẹp dữ liệu lớn sau khi đo bằng migration_user
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
            conn.setAutoCommit(true);
            try (java.sql.Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE maintenance_history DISABLE TRIGGER trg_block_history_mutation");
                st.execute("DELETE FROM maintenance_history WHERE note LIKE 'Bảo trì định kỳ hiệu năng%'");
                st.execute("ALTER TABLE maintenance_history ENABLE TRIGGER trg_block_history_mutation");
            }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM equipment WHERE code LIKE 'PERF_EQ_%'")) {
                ps2.executeUpdate();
            }
        }
    }

    // =========================================================================
    // TASK: CONCURRENCY TEST (≥ 20 ROUNDS WITH COUNTDOWN LATCH)
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("BR-EM-02 Concurrency Test: 2 request tạo WO định kỳ cùng kỳ song song qua CountDownLatch lặp 20 vòng; đúng 1 thành công (201), 1 nhận 409")
    void testConcurrency_PeriodicWorkOrderCreation_20Rounds() throws Exception {
        String mgrToken = fixtures.tokenManager1();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        int totalRounds = 20;

        for (int round = 1; round <= totalRounds; round++) {
            // Chuẩn bị: Đảm bảo thiết bị EQ09 (id 9) không có WO định kỳ mở
            try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
                conn.setAutoCommit(true);
                try (java.sql.Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE maintenance_history DISABLE TRIGGER trg_block_history_mutation");
                    st.execute("DELETE FROM maintenance_history WHERE equipment_id = 9");
                    st.execute("ALTER TABLE maintenance_history ENABLE TRIGGER trg_block_history_mutation");
                    st.execute("DELETE FROM maintenance_work_order WHERE equipment_id = 9 AND type = 'PERIODIC'");
                }
            }

            CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                    .equipmentId(9L)
                    .type(WorkOrderType.PERIODIC)
                    .build();

            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            Callable<Integer> createWoTask = () -> {
                readyLatch.countDown();
                startLatch.await(); // Hai luồng cùng xuất phát tại 1 thời điểm

                try {
                    MvcResult result = mockMvc.perform(post("/api/v1/work-orders")
                                    .header("Authorization", "Bearer " + mgrToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    }
                    return statusCode;
                } catch (Exception e) {
                    return 500;
                }
            };

            Future<Integer> f1 = executor.submit(createWoTask);
            Future<Integer> f2 = executor.submit(createWoTask);

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Cả 2 luồng phải sẵn sàng");
            startLatch.countDown(); // Phát lệnh chạy đồng thời

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);

            // Khẳng định vòng kiểm tra hiện tại: đúng 1 thành công (201) và đúng 1 bị xung đột (409)
            assertEquals(1, successCount.get(), "Vòng " + round + ": Đúng 1 request phải thành công");
            assertEquals(1, conflictCount.get(), "Vòng " + round + ": Đúng 1 request phải bị 409 Conflict");

            // Khẳng định DB: Chỉ có đúng 1 WO định kỳ đang mở cho thiết bị 9
            long openWoCount = workOrderRepository.countByEquipment_EquipmentIdAndTypeAndStatusNot(
                    9L, WorkOrderType.PERIODIC, WorkOrderStatus.CLOSED
            );
            assertEquals(1, openWoCount, "Vòng " + round + ": DB chỉ được có đúng 1 WO định kỳ mở");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
