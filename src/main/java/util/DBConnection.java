package util;

import exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ==============================================================================
 * Centralized DBConnection Utility using JDBC
 * ==============================================================================
 *
 * Provides a standardized, reusable static database connection factory and
 * resource management utilities for native JDBC operations against MySQL 8.x.
 *
 * Requirements demonstrated:
 * - MySQL JDBC Driver (com.mysql.cj.jdbc.Driver)
 * - java.sql.DriverManager
 * - java.sql.Connection
 * - java.sql.PreparedStatement
 * - java.sql.ResultSet
 * - java.sql.SQLException
 * - try-with-resources compatibility
 */
public final class DBConnection {

    private static final DatabaseConnectionManager MANAGER = DatabaseConnectionManager.getInstance();

    private DBConnection() {
        // Prevent instantiation of utility class
    }

    /**
     * Reusable database connection method dispensing active java.sql.Connection instances.
     * Compatible with try-with-resources blocks across DAO classes.
     *
     * @return active java.sql.Connection to MySQL
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return MANAGER.getConnection();
        } catch (DatabaseOperationException e) {
            if (e.getCause() instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            throw new SQLException(e.getMessage(), e);
        }
    }

    /**
     * Parameterized connection method for custom database endpoints.
     *
     * @param url      JDBC connection URL
     * @param username database user
     * @param password database password
     * @return active java.sql.Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(String url, String username, String password) throws SQLException {
        try {
            Class.forName(MANAGER.getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver class not found: " + MANAGER.getDriverClass(), e);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Probes whether the database connection is currently active and reachable.
     *
     * @return true if database is reachable
     */
    public static boolean isConnected() {
        return MANAGER.isDatabaseAvailable();
    }

    /**
     * Safely closes one or more AutoCloseable resources (Connection, PreparedStatement, ResultSet)
     * preventing connection and statement leaks.
     *
     * @param closeables resources to close quietly
     */
    public static void close(AutoCloseable... closeables) {
        DatabaseConnectionManager.closeQuietly(closeables);
    }
}
