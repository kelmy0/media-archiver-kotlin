package com.kelmy.mediaarchiver.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MigrationRunner {
    private static final String MIGRATIONS_DIR = "db/migrations";
    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private MigrationRunner() {
    }

    public static void runPendingMigrations() {
        MigrationRunner.createMigrationsTable();
        MigrationRunner.createMigrationsDirectory();

        try (Connection conn = ConnectionFactory.getConnection()) {
            try (Stream<Path> paths = Files.list(Paths.get(MigrationRunner.MIGRATIONS_DIR))) {
                List<Path> migrationFiles = paths.filter(p -> p.toString().endsWith(".sql")).sorted().toList();

                for (Path file : migrationFiles) {
                    int version = MigrationRunner.getMigrationVersion(file.getFileName().toString());
                    String description = MigrationRunner.getMigrationDescription(file.getFileName().toString());

                    if (!MigrationRunner.isAlreadyImplemented(conn, version)) {
                        log.info("Running migration V{}: {}", version, description);

                        String sqlContent = Files.readString(file);

                        executeMigration(conn, sqlContent, version, description);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Error executing migrations", e);
        }
    }

    private static void executeMigration(Connection conn, String sqlScript, int version, String description) {
        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                String[] statements = sqlScript.split(";");
                for (String sqlStatement : statements) {
                    String trimmed = sqlStatement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            registerMigration(conn, version, description);

            conn.commit();
            log.info("Migration V{} applied and registered successfully.", version);
        } catch (Exception e) {
            try {
                conn.rollback();
                log.error("Rollback executed successfully for failed migration V{}.", version);
            } catch (SQLException rollbackEx) {
                log.error("Failed to execute rollback for migration V{}: {}", version, rollbackEx.getMessage(),
                        rollbackEx);
            }

            throw new RuntimeException("Failed to apply migration V" + version + ": " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                log.error("Failed to restore original auto-commit state", ex);
            }
        }
    }

    private static void createMigrationsDirectory() {
        try {
            Path path = Paths.get(MigrationRunner.MIGRATIONS_DIR);

            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create the migrations directory", e);
        }
    }

    private static int getMigrationVersion(String filename) {
        Pattern pattern = Pattern.compile("^V(\\d+)__");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException(
                "Invalid migration name (must follow the pattern V1__name.sql): " + filename);
    }

    private static String getMigrationDescription(String filename) {
        String noPrefix = filename.replaceFirst("^V\\d+__", "");
        return noPrefix.replace(".sql", "").replace("_", " ");
    }

    private static void createMigrationsTable() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS schema_migrations(
                        version INTEGER PRIMARY KEY,
                        description TEXT NOT NULL,
                        applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """;
        try (Connection conn = ConnectionFactory.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating migrations control table", e);
        }
    }

    private static boolean isAlreadyImplemented(Connection conn, int version) throws SQLException {
        String sql = "SELECT 1 FROM schema_migrations WHERE version = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, version);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void registerMigration(Connection conn, int version, String description) throws SQLException {
        String sql = "INSERT INTO schema_migrations (version, description) VALUES (?,?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, version);
            pstmt.setString(2, description);
            pstmt.executeUpdate();
        }
    }
}
