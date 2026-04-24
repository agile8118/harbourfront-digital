package com.harbourfront;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File-based logger:
 *
 *   info  → logs/YYYY-MM-DD/info.log    format: "YYYY-MM-DD HH:MM:SS -- message"
 *   error → logs/YYYY-MM-DD/errors.log  format: dashed block with name/message/stack
 */
public class Log {

    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DIR_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void info(String message) {
        String dateString = utcNow().format(STAMP_FMT);
        String dir = ensureDir();
        String line = dateString + " -- " + message + "\n";
        append(dir + "/info.log", line);
    }

    public static void error(Throwable t) {
        String dateString = utcNow().format(STAMP_FMT);
        String dir = ensureDir();

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));

        String name    = t.getClass().getSimpleName();
        String msg     = t.getMessage() != null ? t.getMessage() : "";
        String stack   = sw.toString().trim();

        String block =
            "---------------------------------------\n" +
            dateString + "\n" +
            "Error Name: "    + name    + "\n" +
            "Error Message: " + msg     + "\n" +
            "Error Stack:\n"  + stack   + "\n" +
            "---------------------------------------\n";

        append(dir + "/errors.log", block);
    }

    public static void error(String message) {
        String dateString = utcNow().format(STAMP_FMT);
        String dir = ensureDir();

        String block =
            "---------------------------------------\n" +
            dateString + "\n" +
            "Error Message: " + message + "\n" +
            "---------------------------------------\n";

        append(dir + "/errors.log", block);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private static ZonedDateTime utcNow() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    private static String ensureDir() {
        String dateDir = utcNow().format(DIR_FMT);
        String path = "logs/" + dateDir;
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Failed to create log dir: " + e.getMessage());
        }
        return path;
    }

    private static void append(String path, String content) {
        try {
            Files.writeString(
                Paths.get(path), content,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
}