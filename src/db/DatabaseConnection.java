package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConnection – Manages a HikariCP connection pool for thread-safe,
 * high-performance JDBC operations.
 */
public final class DatabaseConnection {

    private static final String DEFAULT_PROPERTIES_FILE = "db.properties";
    private static volatile HikariDataSource dataSource;

    private DatabaseConnection() {}

    private static synchronized void initializeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        Properties props = loadProperties();

        String host     = getEnvOrDefault("SMS_DB_HOST",     props.getProperty("db.host", "localhost"));
        String port     = getEnvOrDefault("SMS_DB_PORT",     props.getProperty("db.port", "3306"));
        String dbName   = getEnvOrDefault("SMS_DB_NAME",     props.getProperty("db.database", "student_management_system"));
        String user     = getEnvOrDefault("SMS_DB_USER",     props.getProperty("db.user", "sms_user"));
        String password = getEnvOrDefault("SMS_DB_PASSWORD", props.getProperty("db.password", "sms_app_password_2026"));

        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, dbName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool tuning
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimumIdle", "2")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));

        // Recommended MySQL performance properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Borrows a connection from the pool. The caller MUST close this connection
     * (preferably with try-with-resources) to return it to the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }

    /**
     * Backward-compatible alias for legacy calls.
     */
    public static Connection getInstance() throws SQLException {
        return getConnection();
    }

    /**
     * Shuts down the pool when application closes.
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        // 1. Try classpath
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(in);
                return properties;
            }
        } catch (Exception ignored) {}

        // 2. Try working directory
        File file = new File(DEFAULT_PROPERTIES_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (Exception ignored) {}
        }
        return properties;
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String val = System.getenv(envVar);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultValue;
    }
}
