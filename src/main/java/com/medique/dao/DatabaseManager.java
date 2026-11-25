package com.medique.dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DEFAULT_DB_NAME = "medique.db";
    private static String dbUrl = null;
    private static Connection keepAliveConnection = null;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    public static synchronized void initializeDatabase() {
        if (dbUrl == null) {
            String userHome = System.getProperty("user.home");
            Path appDir = Paths.get(userHome, ".medique");
            try {
                if (!Files.exists(appDir)) {
                    Files.createDirectories(appDir);
                }
                Path dbPath = appDir.resolve(DEFAULT_DB_NAME);
                dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
            } catch (Exception e) {
                dbUrl = "jdbc:sqlite:" + DEFAULT_DB_NAME;
            }
        }
        applySchema();
    }

    public static synchronized void setDatabaseUrlForTesting(String customUrl) {
        if (keepAliveConnection != null) {
            try {
                keepAliveConnection.close();
            } catch (Exception ignored) {}
            keepAliveConnection = null;
        }
        dbUrl = customUrl;
        try {
            // Keep a connection open so in-memory SQLite instances are not destroyed on close
            keepAliveConnection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = keepAliveConnection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open test connection: " + e.getMessage(), e);
        }
        applySchema();
    }

    public static Connection getConnection() throws SQLException {
        if (dbUrl == null) {
            initializeDatabase();
        }
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private static void applySchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = DatabaseManager.class.getResourceAsStream("/schema.sql")) {

            if (is == null) {
                throw new IllegalStateException("schema.sql resource not found on classpath");
            }

            String fullSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Strip single-line comments
            String cleanSql = fullSql.replaceAll("(?m)--.*$", "");

            // Split into individual SQL statements
            String[] statements = cleanSql.split(";");
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Medique database schema: " + e.getMessage(), e);
        }
    }
}
