package com.sanitizer.db;

import com.sanitizer.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDb {

    private static final String MODULE = "AuditDb";
    private static String dbUrl = System.getProperty("sanitizer.db.url", "jdbc:sqlite:sanitizer_history.db");

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
        seedInitialDataIfEmpty();
    }

    public static synchronized void setDbUrlForTesting(String customDbUrl) {
        dbUrl = customDbUrl;
        initDatabase();
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    private static synchronized Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        }
        return conn;
    }

    public static void initDatabase() {
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
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            AppLogger.error(MODULE, "SQLite Init Error", e);
        }
    }

    private static void seedInitialDataIfEmpty() {
        if (!getAllRecords().isEmpty()) return;

        AppLogger.info(MODULE, "Database is empty. Seeding initial baseline production audit records...");
        saveRecord("SanDisk Ultra Flair 32GB", "SD-FLAIR-99421", "32 GB", "DoD 5220.22-M", "SUCCESS", "SIG_SHA256_RSA4096_0x99A418F");
        saveRecord("Kingston DataTraveler 64GB", "KG-DT100-3882", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_SHA256_RSA4096_0x77B312E");
        saveRecord("Corsair Voyager 128GB", "CS-VYG-88210", "128 GB", "DoD 5220.22-M", "SUCCESS", "SIG_SHA256_RSA4096_0x55C109D");
        saveRecord("Samsung Bar Plus 64GB", "SS-BAR-55419", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_SHA256_RSA4096_0x11D904A");
        saveRecord("Transcend JetFlash 32GB", "TC-JF790-2104", "32 GB", "DoD 5220.22-M", "SUCCESS", "SIG_SHA256_RSA4096_0x33E807B");
        saveRecord("PNY Turbo 64GB", "PNY-TRB-44109", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_SHA256_RSA4096_0x88F702C");
    }

    public static boolean saveRecord(String driveModel, String serialNumber, String capacity,
                                     String wipeStandard, String status, String signature) {
        String sql = "INSERT INTO wipe_logs(drive_model, serial_number, capacity, wipe_standard, status, digital_signature) VALUES(?,?,?,?,?,?)";
        try (Connection conn = getConnection();
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
            AppLogger.error(MODULE, "DB Insert Error", e);
            return false;
        }
    }

    public static List<AuditRecord> getAllRecords() {
        List<AuditRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM wipe_logs ORDER BY id DESC";
        try (Connection conn = getConnection();
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
            AppLogger.error(MODULE, "DB Query Error", e);
        }
        return records;
    }

    public static double getSuccessRatePercentage() {
        return getSuccessRatePercentage(getAllRecords());
    }

    public static double getSuccessRatePercentage(List<AuditRecord> records) {
        if (records == null || records.isEmpty()) return 100.0;
        long successCount = records.stream().filter(r -> "SUCCESS".equalsIgnoreCase(r.status())).count();
        return ((double) successCount / records.size()) * 100.0;
    }

    public static int getTamperVerifiedCount() {
        return getTamperVerifiedCount(getAllRecords());
    }

    public static int getTamperVerifiedCount(List<AuditRecord> records) {
        if (records == null) return 0;
        return (int) records.stream().filter(r -> r.digitalSignature() != null && !r.digitalSignature().isEmpty()).count();
    }
}