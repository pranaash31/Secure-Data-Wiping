package com.sanitizer.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDb {

    private static final String DB_URL = "jdbc:sqlite:sanitizer_history.db";

    public record AuditRecord(
            int id,
            String timestamp,
            String driveModel,
            String serialNumber,
            String capacity,
            String wipeStandard,
            String status,
            String digitalSignature
    ) {}

    static {
        initDatabase();
    }

    private static void initDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS wipe_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                drive_model TEXT NOT NULL,
                serial_number TEXT NOT NULL,
                capacity TEXT NOT NULL,
                wipe_standard TEXT NOT NULL,
                status TEXT NOT NULL,
                digital_signature TEXT NOT NULL
            );
            """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("SQLite Init Error: " + e.getMessage());
        }
    }

    public static boolean saveRecord(String driveModel, String serialNumber, String capacity,
                                     String wipeStandard, String status, String signature) {
        String sql = "INSERT INTO wipe_logs(drive_model, serial_number, capacity, wipe_standard, status, digital_signature) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, driveModel);
            pstmt.setString(2, serialNumber);
            pstmt.setString(3, capacity);
            pstmt.setString(4, wipeStandard);
            pstmt.setString(5, status);
            pstmt.setString(6, signature);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("DB Insert Error: " + e.getMessage());
            return false;
        }
    }

    public static List<AuditRecord> getAllRecords() {
        List<AuditRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM wipe_logs ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new AuditRecord(
                        rs.getInt("id"),
                        rs.getString("timestamp"),
                        rs.getString("drive_model"),
                        rs.getString("serial_number"),
                        rs.getString("capacity"),
                        rs.getString("wipe_standard"),
                        rs.getString("status"),
                        rs.getString("digital_signature")
                ));
            }
        } catch (SQLException e) {
            System.err.println("DB Query Error: " + e.getMessage());
        }
        return records;
    }
}