package service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import utils.MyDatabase;

public class LoginAttemptService {

    private static LoginAttemptService instance;

    private Map<String, LoginAttempt> attemptCache = new HashMap<>();

    private static final int MAX_ATTEMPTS_BEFORE_COOLDOWN = 3;
    private static final int MAX_ATTEMPTS_BEFORE_BAN = 5;
    private static final int COOLDOWN_SECONDS = 30;
    private static final int BAN_MINUTES = 15;

    private LoginAttemptService() {
        initializeTable();
    }

    public static synchronized LoginAttemptService getInstance() {
        if (instance == null) {
            instance = new LoginAttemptService();
        }
        return instance;
    }

    // ✅ FIXED TABLE CREATION
    private void initializeTable() {

        String sql = "CREATE TABLE IF NOT EXISTS login_attempts (" +
                "email VARCHAR(255) PRIMARY KEY," +
                "attempt_count INT DEFAULT 0," +
                "last_attempt_time DATETIME NULL," +
                "cooldown_until DATETIME NULL," +
                "banned_until DATETIME NULL" +
                ")";

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = MyDatabase.getInstance().getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            System.out.println("✅ Login attempts table initialized");

        } catch (SQLException e) {
            System.err.println("❌ Failed to create login_attempts table: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) {}
        }
    }

    public LoginStatus checkLoginAllowed(String email) {

        LoginAttempt attempt = getAttempt(email);

        if (attempt == null) {
            return new LoginStatus(true, 0, 0, null);
        }

        LocalDateTime now = LocalDateTime.now();

        if (attempt.bannedUntil != null && now.isBefore(attempt.bannedUntil)) {
            long secondsRemaining =
                    Math.max(0, java.time.Duration.between(now, attempt.bannedUntil).toMillis() / 1000);
            return new LoginStatus(false, attempt.attemptCount, secondsRemaining, "BANNED");
        }

        if (attempt.cooldownUntil != null && now.isBefore(attempt.cooldownUntil)) {
            long secondsRemaining =
                    Math.max(0, java.time.Duration.between(now, attempt.cooldownUntil).toMillis() / 1000);
            return new LoginStatus(false, attempt.attemptCount, secondsRemaining, "COOLDOWN");
        }

        return new LoginStatus(true, attempt.attemptCount, 0, null);
    }

    public LoginStatus recordFailedAttempt(String email) {

        LoginAttempt attempt = getAttempt(email);

        if (attempt == null) {
            attempt = new LoginAttempt(email);
        }

        attempt.attemptCount++;
        attempt.lastAttemptTime = LocalDateTime.now();

        if (attempt.attemptCount == MAX_ATTEMPTS_BEFORE_COOLDOWN) {
            attempt.cooldownUntil = LocalDateTime.now().plusSeconds(COOLDOWN_SECONDS);
            saveAttempt(attempt);
            return new LoginStatus(false, attempt.attemptCount, COOLDOWN_SECONDS, "COOLDOWN");
        }

        if (attempt.attemptCount >= MAX_ATTEMPTS_BEFORE_BAN) {
            attempt.bannedUntil = LocalDateTime.now().plusMinutes(BAN_MINUTES);
            attempt.cooldownUntil = null;
            saveAttempt(attempt);
            return new LoginStatus(false, attempt.attemptCount, BAN_MINUTES * 60, "BANNED");
        }

        saveAttempt(attempt);
        return new LoginStatus(true, attempt.attemptCount, 0, null);
    }

    public void resetAttempts(String email) {

        String sql = "DELETE FROM login_attempts WHERE email = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = MyDatabase.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.executeUpdate();

            System.out.println("✅ Reset login attempts for: " + email);

        } catch (SQLException e) {
            System.err.println("❌ Failed to reset attempts: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) {}
        }
    }

    private LoginAttempt getAttempt(String email) {

        String sql = "SELECT * FROM login_attempts WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = MyDatabase.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                LoginAttempt attempt = new LoginAttempt(email);
                attempt.attemptCount = rs.getInt("attempt_count");

                Timestamp lastAttempt = rs.getTimestamp("last_attempt_time");
                if (lastAttempt != null)
                    attempt.lastAttemptTime = lastAttempt.toLocalDateTime();

                Timestamp cooldown = rs.getTimestamp("cooldown_until");
                if (cooldown != null)
                    attempt.cooldownUntil = cooldown.toLocalDateTime();

                Timestamp banned = rs.getTimestamp("banned_until");
                if (banned != null)
                    attempt.bannedUntil = banned.toLocalDateTime();

                return attempt;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get attempt: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) {}
        }

        return null;
    }

    private void saveAttempt(LoginAttempt attempt) {

        String sql = "INSERT INTO login_attempts (email, attempt_count, last_attempt_time, cooldown_until, banned_until) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "attempt_count = VALUES(attempt_count), " +
                "last_attempt_time = VALUES(last_attempt_time), " +
                "cooldown_until = VALUES(cooldown_until), " +
                "banned_until = VALUES(banned_until)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = MyDatabase.getInstance().getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, attempt.email);
            stmt.setInt(2, attempt.attemptCount);
            stmt.setTimestamp(3, attempt.lastAttemptTime != null ? Timestamp.valueOf(attempt.lastAttemptTime) : null);
            stmt.setTimestamp(4, attempt.cooldownUntil != null ? Timestamp.valueOf(attempt.cooldownUntil) : null);
            stmt.setTimestamp(5, attempt.bannedUntil != null ? Timestamp.valueOf(attempt.bannedUntil) : null);

            stmt.executeUpdate();


        } catch (SQLException e) {
            System.err.println("❌ Failed to save attempt: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) {}
        }
    }

    private static class LoginAttempt {
        String email;
        int attemptCount;
        LocalDateTime lastAttemptTime;
        LocalDateTime cooldownUntil;
        LocalDateTime bannedUntil;

        LoginAttempt(String email) {
            this.email = email;
            this.attemptCount = 0;
        }
    }

    public static class LoginStatus {
        public final boolean allowed;
        public final int attemptCount;
        public final long secondsRemaining;
        public final String status;

        public LoginStatus(boolean allowed, int attemptCount, long secondsRemaining, String status) {
            this.allowed = allowed;
            this.attemptCount = attemptCount;
            this.secondsRemaining = secondsRemaining;
            this.status = status;
        }
        public String getFormattedTime() {
            long minutes = secondsRemaining / 60;
            long seconds = secondsRemaining % 60;

            if (minutes > 0) {
                return minutes + "m " + seconds + "s";
            } else {
                return seconds + "s";
            }
        }
    }
}