package com.harbourfront.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Seed {

    public static void run() {
        String host   = env("DB_HOST",     "localhost");
        String port   = env("DB_PORT",     "5432");
        String dbName = env("DB_NAME",     "harbourfront");
        String user   = env("DB_USER",     "postgres");
        String pass   = env("DB_PASSWORD", "");

        String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        String dbUrl    = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

        // ── Step 1: create database if it doesn't exist ────────────────────────
        System.out.println("[postgres] checking database '" + dbName + "'...");
        try (Connection admin = DriverManager.getConnection(adminUrl, user, pass)) {
            ResultSet rs = admin.createStatement().executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + dbName.replace("'", "''") + "'"
            );
            if (!rs.next()) {
                admin.createStatement().execute("CREATE DATABASE \"" + dbName + "\"");
                System.out.println("[postgres] created database: " + dbName);
            } else {
                System.out.println("[postgres] database '" + dbName + "' already exists.");
            }
        } catch (SQLException e) {
            System.err.println("[postgres] failed to check/create database: " + e.getMessage());
            System.exit(1);
        }

        // ── Step 2: connect to target database, drop & recreate tables ─────────
        try (Connection conn = DriverManager.getConnection(dbUrl, user, pass)) {
            System.out.println("[postgres] connected to database: " + dbName);

            // Read table filenames in declared order from the manifest
            List<String> tableFiles = readManifest();

            // Drop in reverse order to respect foreign-key dependencies
            System.out.println("\nDropping tables...");
            for (int i = tableFiles.size() - 1; i >= 0; i--) {
                String sql       = readResource("/database/tables/" + tableFiles.get(i));
                String tableName = extractTableName(sql);
                conn.createStatement().execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
                System.out.println("[postgres] " + tableName + " table was dropped.");
            }

            // Create in forward order
            System.out.println("\nCreating tables...");
            for (String filename : tableFiles) {
                String sql       = readResource("/database/tables/" + filename);
                String tableName = extractTableName(sql);
                conn.createStatement().execute(sql);
                System.out.println("[postgres] " + tableName + " table was created successfully.");
            }

            // Run triggers (skip if file is comments-only)
            String triggerSQL = readResource("/database/triggers.sql");
            if (hasSqlContent(triggerSQL)) {
                System.out.println("\nSetting up triggers...");
                conn.createStatement().execute(triggerSQL);
                System.out.println("[postgres] triggers were set up successfully.");
            }

        } catch (SQLException | IOException e) {
            System.err.println("[postgres] seed failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\nSeed complete.");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static List<String> readManifest() throws IOException {
        try (InputStream is = Seed.class.getResourceAsStream("/database/tables/manifest.txt")) {
            if (is == null) throw new IOException("manifest.txt not found on classpath");
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .collect(Collectors.toList());
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = Seed.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractTableName(String sql) {
        Pattern p = Pattern.compile(
            "CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(sql);
        if (m.find()) return m.group(1);
        throw new RuntimeException("Could not determine table name from SQL:\n" + sql);
    }

    private static boolean hasSqlContent(String sql) {
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("--")) return true;
        }
        return false;
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}