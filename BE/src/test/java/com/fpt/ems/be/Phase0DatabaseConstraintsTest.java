package com.fpt.ems.be;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase0DatabaseConstraintsTest {

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

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
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

    @Test
    @Order(1)
    @DisplayName("(a) Insert WO định kỳ thứ 2 cùng kỳ chưa đóng -> Vi phạm partial unique index UQ-05")
    void testA_DuplicatePeriodicWorkOrderFailsPartialUniqueIndex() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
            String sql = "INSERT INTO maintenance_work_order (wo_code, equipment_id, plan_id, type, period, status) " +
                    "VALUES ('WO_DUP_PERIODIC', 1, 1, 'PERIODIC', '2026-Q1', 'OPEN')";

            SQLException ex = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                }
            });

            assertEquals("23505", ex.getSQLState(), "Expected unique violation SQLState 23505");
            assertTrue(ex.getMessage().contains("uq_periodic_wo_open"),
                    "Expected violation of index uq_periodic_wo_open, actual message: " + ex.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("(b1) UPDATE & DELETE trên maintenance_history và audit_log bằng app_user -> Lỗi Permission Denied (42501)")
    void testB1_AppUserSqlPermissionDeniedOnAppendOnlyTables() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "app_user", APP_USER_PASSWORD)) {
            SQLException exUpdateHistory = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE maintenance_history SET note = 'Tampered Note' WHERE history_id = 1");
                }
            });
            assertEquals("42501", exUpdateHistory.getSQLState(), "Expected 42501 permission denied for UPDATE maintenance_history");

            SQLException exDeleteHistory = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM maintenance_history WHERE history_id = 1");
                }
            });
            assertEquals("42501", exDeleteHistory.getSQLState(), "Expected 42501 permission denied for DELETE maintenance_history");

            SQLException exUpdateAudit = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE audit_log SET reason_code = 'HACKED' WHERE log_id = 1");
                }
            });
            assertEquals("42501", exUpdateAudit.getSQLState(), "Expected 42501 permission denied for UPDATE audit_log");

            SQLException exDeleteAudit = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM audit_log WHERE log_id = 1");
                }
            });
            assertEquals("42501", exDeleteAudit.getSQLState(), "Expected 42501 permission denied for DELETE audit_log");
        }
    }

    @Test
    @Order(3)
    @DisplayName("(b2) Trigger chặn UPDATE & DELETE trên maintenance_history và audit_log khi chạy bằng migration_user")
    void testB2_TriggerBlocksAppendOnlyMutationForPrivilegedUser() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO maintenance_history (equipment_id, action, note) VALUES (1, 'TRIGGER_TEST', 'Original note')");
            }

            SQLException exUpdateHistory = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE maintenance_history SET note = 'Modified' WHERE action = 'TRIGGER_TEST'");
                }
            });
            assertEquals("55000", exUpdateHistory.getSQLState());
            assertTrue(exUpdateHistory.getMessage().contains("is append-only"),
                    "Expected append-only message, got: " + exUpdateHistory.getMessage());

            SQLException exDeleteHistory = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM maintenance_history WHERE action = 'TRIGGER_TEST'");
                }
            });
            assertEquals("55000", exDeleteHistory.getSQLState());
            assertTrue(exDeleteHistory.getMessage().contains("is append-only"));

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO audit_log (action, outcome) VALUES ('AUDIT_TRIGGER_TEST', 'SUCCESS')");
            }

            SQLException exUpdateAudit = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE audit_log SET outcome = 'FAILED' WHERE action = 'AUDIT_TRIGGER_TEST'");
                }
            });
            assertEquals("55000", exUpdateAudit.getSQLState());
            assertTrue(exUpdateAudit.getMessage().contains("is append-only"));

            SQLException exDeleteAudit = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM audit_log WHERE action = 'AUDIT_TRIGGER_TEST'");
                }
            });
            assertEquals("55000", exDeleteAudit.getSQLState());
            assertTrue(exDeleteAudit.getMessage().contains("is append-only"));
        }
    }

    @Test
    @Order(4)
    @DisplayName("(c) UPDATE & DELETE trên inspection_result của WO đã CLOSED -> Lỗi Trigger")
    void testC_ClosedWorkOrderImmutabilityTrigger() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, migrationUsername, migrationPassword)) {

            SQLException exUpdateResult = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE inspection_result SET condition = 'Hacked' WHERE wo_id = 4");
                }
            });
            assertEquals("55000", exUpdateResult.getSQLState());
            assertTrue(exUpdateResult.getMessage().contains("is CLOSED and immutable"),
                    "Expected CLOSED and immutable message, got: " + exUpdateResult.getMessage());

            SQLException exDeleteResult = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM inspection_result WHERE wo_id = 4");
                }
            });
            assertEquals("55000", exDeleteResult.getSQLState());
            assertTrue(exDeleteResult.getMessage().contains("is CLOSED and immutable"));

            SQLException exUpdateWo = assertThrows(SQLException.class, () -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE maintenance_work_order SET status = 'IN_PROGRESS' WHERE wo_id = 4");
                }
            });
            assertEquals("55000", exUpdateWo.getSQLState());
            assertTrue(exUpdateWo.getMessage().contains("is CLOSED and immutable"));

            try (Statement stmt = conn.createStatement()) {
                int rowsUpdated = stmt.executeUpdate(
                        "UPDATE maintenance_work_order SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP WHERE wo_id = 3");
                assertEquals(1, rowsUpdated, "Expected successful transition from SUBMITTED to CLOSED");
            }
        }
    }
}
