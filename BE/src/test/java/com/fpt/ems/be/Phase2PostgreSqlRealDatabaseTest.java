package com.fpt.ems.be;

import com.fpt.ems.be.model.AuditLog;
import com.fpt.ems.be.model.Equipment;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.AuditLogRepository;
import com.fpt.ems.be.repository.EquipmentRepository;
import com.fpt.ems.be.service.AuditService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase2PostgreSqlRealDatabaseTest {

    private static PostgreSQLContainer<?> postgresContainer;
    private static String jdbcUrl;
    private static String migrationUsername;
    private static String migrationPassword;
    private static final String APP_USER_PASSWORD = "test_app_user_pass_123";
    private static boolean isLocalClusterStarted = false;
    private static File localDataDir;
    private static int localPort;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    @Order(1)
    @DisplayName("PostgreSQL thật: Audit STATE_VIOLATION/DENIED còn lại trong DB sau khi transaction nghiệp vụ rollback (REQUIRES_NEW)")
    void testAuditRequiresNew_PersistsAfterRollback() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> {
            txTemplate.execute(status -> {
                 auditService.recordDenied(
                        "STATE_VIOLATION",
                        "WORK_ORDER",
                        "999",
                        AuditOutcome.DENIED,
                        "WO_PERIOD_DUPLICATE",
                        null,
                        Map.of("reason", "Thiết bị đã có work order định kỳ")
                );

                 throw new RuntimeException("Business transaction failed!");
            });
        });

         List<AuditLog> logs = auditLogRepository.findByActionOrderByOccurredAtDesc("STATE_VIOLATION");
        assertFalse(logs.isEmpty(), "Audit log must remain committed in DB after transaction rollback");
        assertEquals("WO_PERIOD_DUPLICATE", logs.get(0).getReasonCode());
    }

    @Test
    @Order(2)
    @DisplayName("PostgreSQL thật: Partial Unique Index (uq_periodic_wo_open) chặn trùng kỳ WO định kỳ ở tầng DB")
    void testPartialUniqueIndex_BlocksDuplicateOpenPeriodicWo() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
             try (PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO maintenance_work_order (wo_code, equipment_id, plan_id, type, period, status, created_at) " +
                    "VALUES ('WO-TEST-REAL-01', 6, 6, 'PERIODIC', '2027-Q1', 'OPEN', now())")) {
                ps1.executeUpdate();
            }

              try (PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO maintenance_work_order (wo_code, equipment_id, plan_id, type, period, status, created_at) " +
                    "VALUES ('WO-TEST-REAL-02', 6, 6, 'PERIODIC', '2027-Q1', 'ASSIGNED', now())")) {
                SQLException ex = assertThrows(SQLException.class, ps2::executeUpdate);
                assertTrue(ex.getMessage().contains("uq_periodic_wo_open"),
                        "Expected constraint violation on uq_periodic_wo_open, got: " + ex.getMessage());
            }

             try (PreparedStatement ps3 = conn.prepareStatement(
                    "INSERT INTO maintenance_work_order (wo_code, equipment_id, plan_id, type, period, status, created_at, closed_at) " +
                    "VALUES ('WO-TEST-REAL-03', 6, 6, 'PERIODIC', '2027-Q1', 'CLOSED', now(), now())")) {
                assertDoesNotThrow(() -> { ps3.executeUpdate(); }, "CLOSED periodic WO must not be blocked by partial unique index");
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("PostgreSQL thật: Row Locking (SELECT ... FOR UPDATE) bảo đảm tuần tự hóa khi hai transaction tranh chấp tài nguyên")
    void testRowLocking_PessimisticWrite() throws Exception {
        TransactionTemplate tx1 = new TransactionTemplate(transactionManager);
        TransactionTemplate tx2 = new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch tx1FinishedLatch = new CountDownLatch(1);

        Future<?> f1 = executor.submit(() -> {
            tx1.execute(status -> {
                Equipment eq = equipmentRepository.findByIdWithLock(1L).orElseThrow();
                lockAcquiredLatch.countDown();  
                try {
                    Thread.sleep(600);  
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                eq.setBrand("BrandLockedByTx1");
                equipmentRepository.save(eq);
                return null;
            });
            tx1FinishedLatch.countDown();
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                lockAcquiredLatch.await();  
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long startTime = System.currentTimeMillis();
            tx2.execute(status -> {
                 Equipment eq = equipmentRepository.findByIdWithLock(1L).orElseThrow();
                long elapsed = System.currentTimeMillis() - startTime;
                assertTrue(elapsed >= 400, "Thread 2 should have been blocked waiting for row lock release");
                return null;
            });
        });

        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdown();
    }
}
