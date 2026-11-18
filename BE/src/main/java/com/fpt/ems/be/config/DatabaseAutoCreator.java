package com.fpt.ems.be.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Chạy TRƯỚC khi Spring context khởi động (kể cả trước Flyway).
 * Tự động tạo database PostgreSQL nếu chưa tồn tại, giúp dev
 * không cần tạo DB thủ công trước khi chạy BE lần đầu.
 *
 * Đăng ký qua: META-INF/spring.factories
 */
public class DatabaseAutoCreator implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAutoCreator.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Đọc config (hỗ trợ cả biến môi trường lẫn application.yaml defaults)
        String dbHost = resolve(environment, "DB_HOST", "localhost");
        String dbPort = resolve(environment, "DB_PORT", "5432");
        String dbName = resolve(environment, "DB_NAME", "ems_db");
        String user   = resolve(environment, "MIGRATION_USER",     "postgres");
        String pass   = resolve(environment, "MIGRATION_PASSWORD", "postgres123");

        // Kết nối tới DB mặc định "postgres" để tạo database mục tiêu
        String adminUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.warn("[DatabaseAutoCreator] PostgreSQL driver not found, skipping auto-create.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(adminUrl, user, pass)) {
            // Kiểm tra database đã tồn tại chưa
            boolean exists = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
                exists = rs.next();
            }

            if (!exists) {
                log.info("[DatabaseAutoCreator] Database '{}' không tồn tại. Đang tạo...", dbName);
                try (Statement st = conn.createStatement()) {
                    st.execute("CREATE DATABASE \"" + dbName + "\"");
                }
                log.info("[DatabaseAutoCreator] ✅ Đã tạo database '{}'", dbName);
            } else {
                log.info("[DatabaseAutoCreator] Database '{}' đã tồn tại, bỏ qua.", dbName);
            }
        } catch (Exception e) {
            // Không ném exception — để lỗi Flyway/datasource thật sự báo sau
            log.warn("[DatabaseAutoCreator] Không thể tự tạo database '{}': {}", dbName, e.getMessage());
            log.warn("[DatabaseAutoCreator] Hãy kiểm tra MIGRATION_USER có quyền CREATEDB, hoặc tạo DB thủ công.");
        }
    }

    /** Lấy giá trị từ environment, fallback về defaultValue nếu null/rỗng. */
    private String resolve(ConfigurableEnvironment env, String key, String defaultValue) {
        String val = env.getProperty(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    @Override
    public int getOrder() {
        // Chạy sớm nhất (Integer.MIN_VALUE = ưu tiên cao nhất)
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
