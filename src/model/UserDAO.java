package model;

import db.DatabaseConnection;
import util.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * UserDAO – Data Access Object for User authentication and account state.
 */
public class UserDAO {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.id, u.person_id, u.username, u.password_hash, u.role, "
                   + "u.is_active, u.failed_attempts, u.locked_until, u.created_at, u.last_login, "
                   + "l.id AS lecturer_id "
                   + "FROM users u "
                   + "LEFT JOIN lecturers l ON l.person_id = u.person_id "
                   + "WHERE u.username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    int pid = rs.getInt("person_id");
                    u.setPersonId(rs.wasNull() ? null : pid);
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRole(SessionManager.Role.valueOf(rs.getString("role")));
                    u.setActive(rs.getBoolean("is_active"));
                    u.setFailedAttempts(rs.getInt("failed_attempts"));
                    Timestamp locked = rs.getTimestamp("locked_until");
                    if (locked != null) u.setLockedUntil(locked.toLocalDateTime());
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) u.setCreatedAt(created.toLocalDateTime());
                    Timestamp login = rs.getTimestamp("last_login");
                    if (login != null) u.setLastLogin(login.toLocalDateTime());

                    int lid = rs.getInt("lecturer_id");
                    u.setLecturerId(rs.wasNull() ? null : lid);
                    return u;
                }
            }
        }
        return null;
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void insert(User user) throws SQLException {
        String sql = "INSERT INTO users (person_id, username, password_hash, role, is_active) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (user.getPersonId() != null) ps.setInt(1, user.getPersonId());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
        }
    }

    public void recordFailedAttempt(int userId, int newCount, LocalDateTime lockedUntil) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newCount);
            if (lockedUntil != null) ps.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            else ps.setNull(2, Types.TIMESTAMP);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void recordSuccessfulLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL, last_login = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
