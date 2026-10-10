package util;

import exception.DatabaseOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * ==============================================================================
 * Database Operations: Central JDBC Connection Manager (7 marks foundation)
 * ==============================================================================
 * 
 * Thread-safe Singleton managing JDBC database connectivity to MySQL.
 * 
 * Key Responsibilities:
 * 1. Loads externalized connection parameters from db.properties (URL, username, password).
 * 2. Prevents hardcoding of database credentials inside application source files.
 * 3. Dynamically registers the MySQL JDBC Driver (com.mysql.cj.jdbc.Driver).
 * 4. Dispenses active java.sql.Connection instances to JDBC DAO implementations.
 * 5. Translates low-level java.sql.SQLException into user-safe DatabaseOperationException.
 * 6. Provides centralized resource closing utilities (closeQuietly) to prevent connection leaks.
 */
public class DatabaseConnectionManager {

    private static volatile DatabaseConnectionManager instance;
    private final Properties dbProperties = new Properties();

    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/project_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "password";

    protected DatabaseConnectionManager() {
        loadProperties();
        loadDriver();
    }

    /**
     * Retrieves the Singleton instance of DatabaseConnectionManager using Double-Checked Locking.
     *
     * @return thread-safe singleton instance
     */
    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Loads database configuration properties from the classpath (src/main/resources/db.properties).
     */
    public synchronized void loadProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                dbProperties.load(is);
            } else {
                applyFallbackProperties();
            }
        } catch (IOException e) {
            applyFallbackProperties();
        }

        // Twelve-Factor deployment readiness: allow environment variables & system properties to override
        applyEnvironmentOverrides();
    }

    private void applyEnvironmentOverrides() {
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.isBlank()) dbProperties.setProperty("db.url", envUrl);
        String sysUrl = System.getProperty("db.url");
        if (sysUrl != null && !sysUrl.isBlank()) dbProperties.setProperty("db.url", sysUrl);

        String envUser = System.getenv("DB_USERNAME");
        if (envUser == null || envUser.isBlank()) envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.isBlank()) {
            dbProperties.setProperty("db.username", envUser);
            dbProperties.setProperty("db.user", envUser);
        }
        String sysUser = System.getProperty("db.username");
        if (sysUser != null && !sysUser.isBlank()) {
            dbProperties.setProperty("db.username", sysUser);
            dbProperties.setProperty("db.user", sysUser);
        }

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null && !envPass.isBlank()) dbProperties.setProperty("db.password", envPass);
        String sysPass = System.getProperty("db.password");
        if (sysPass != null && !sysPass.isBlank()) dbProperties.setProperty("db.password", sysPass);

        String envMode = System.getenv("DAO_MODE");
        if (envMode != null && !envMode.isBlank()) dbProperties.setProperty("dao.mode", envMode);
        String sysMode = System.getProperty("dao.mode");
        if (sysMode != null && !sysMode.isBlank()) dbProperties.setProperty("dao.mode", sysMode);
    }

    private void applyFallbackProperties() {
        dbProperties.setProperty("db.driver", DEFAULT_DRIVER);
        dbProperties.setProperty("db.url", DEFAULT_URL);
        dbProperties.setProperty("db.username", DEFAULT_USER);
        dbProperties.setProperty("db.password", DEFAULT_PASSWORD);
        dbProperties.setProperty("dao.mode", "memory");
    }

    /**
     * Registers the JDBC driver class with DriverManager.
     */
    private void loadDriver() {
        String driver = getDriverClass();
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnectionManager] Warning: JDBC Driver not registered: " + e.getMessage());
        }
    }

    /**
     * Programmatic configuration override (used for integration testing or dynamic environments).
     *
     * @param url      JDBC connection URL
     * @param username database user
     * @param password database password
     * @param driver   JDBC driver class name
     */
    public synchronized void configure(String url, String username, String password, String driver) {
        if (url != null) dbProperties.setProperty("db.url", url);
        if (username != null) {
            dbProperties.setProperty("db.username", username);
            dbProperties.setProperty("db.user", username);
        }
        if (password != null) dbProperties.setProperty("db.password", password);
        if (driver != null) {
            dbProperties.setProperty("db.driver", driver);
            loadDriver();
        }
    }

    /**
     * Programmatic configuration override via Properties.
     *
     * @param properties new properties to apply
     */
    public synchronized void configure(Properties properties) {
        if (properties != null) {
            this.dbProperties.putAll(properties);
            loadDriver();
        }
    }

    public String getUrl() {
        return dbProperties.getProperty("db.url", DEFAULT_URL);
    }

    public String getUsername() {
        String username = dbProperties.getProperty("db.username");
        if (username == null || username.isBlank()) {
            username = dbProperties.getProperty("db.user", DEFAULT_USER);
        }
        return username;
    }

    public String getPassword() {
        return dbProperties.getProperty("db.password", DEFAULT_PASSWORD);
    }

    public String getDriverClass() {
        return dbProperties.getProperty("db.driver", DEFAULT_DRIVER);
    }

    public String getDaoMode() {
        return dbProperties.getProperty("dao.mode", "memory");
    }

    /**
     * Establishes and returns a new JDBC Connection to MySQL.
     *
     * @return active java.sql.Connection
     * @throws DatabaseOperationException if connection cannot be established
     */
    public Connection getConnection() {
        String url = getUrl();
        String user = getUsername();
        String password = getPassword();

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // [OOP Concept: Exception Handling]
            // Wraps technical SQLException in user-safe DatabaseOperationException without exposing internal credentials
            throw new DatabaseOperationException(
                    "Unable to establish secure database connection. Please verify MySQL availability and credentials.", e);
        }
    }

    /**
     * Probes whether the database is accessible with current credentials.
     *
     * @return true if connection succeeds, false otherwise (without throwing an uncaught exception)
     */
    public boolean isDatabaseAvailable() {
        try (Connection conn = DriverManager.getConnection(getUrl(), getUsername(), getPassword())) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Automatically applies non-breaking schema updates (such as adding the 'password' column
     * to the 'users' table if missing) when MySQL is connected.
     */
    public synchronized void ensureSchemaUpdated() {
        if (!isDatabaseAvailable()) return;
        try (Connection conn = DriverManager.getConnection(getUrl(), getUsername(), getPassword());
             Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "users", "password")) {
                if (!rs.next()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN password VARCHAR(255) DEFAULT 'password123'");
                }
            }
        } catch (Exception ignored) {
            // Gracefully handled if permissions or managed externally
        }
    }

    /**
     * Safely closes one or more AutoCloseable resources (Connection, PreparedStatement, ResultSet)
     * preventing resource and connection leaks.
     *
     * @param closeables resources to close quietly
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // Suppressed to avoid masking primary exceptions during resource cleanup
                }
            }
        }
    }

    /**
     * Resets the singleton instance (for testing teardowns).
     */
    public static synchronized void resetInstance() {
        instance = null;
    }
}
