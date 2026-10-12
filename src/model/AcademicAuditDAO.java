package model;

import db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AcademicAuditDAO - Manages audit records for all academic exceptions and overrides.
 */
public class AcademicAuditDAO {

    public void log(AcademicAuditLog entry) throws SQLException {
        String sql = "INSERT INTO academic_audit_log (entity_type, entity_id, student_id, course_id, " +
                "academic_year, semester, action, previous_value, new_value, reason, authorized_by, authorized_user) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getEntityType());
            if (entry.getEntityId() != null) ps.setInt(2, entry.getEntityId()); else ps.setNull(2, Types.INTEGER);
            if (entry.getStudentId() != null) ps.setInt(3, entry.getStudentId()); else ps.setNull(3, Types.INTEGER);
            if (entry.getCourseId() != null) ps.setInt(4, entry.getCourseId()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, entry.getAcademicYear());
            ps.setInt(6, entry.getSemester());
            ps.setString(7, entry.getAction());
            ps.setString(8, entry.getPreviousValue());
            ps.setString(9, entry.getNewValue());
            ps.setString(10, entry.getReason());
            if (entry.getAuthorizedBy() != null) ps.setInt(11, entry.getAuthorizedBy()); else ps.setNull(11, Types.INTEGER);
            ps.setString(12, entry.getAuthorizedUser());
            ps.executeUpdate();
        }
    }

    public List<AcademicAuditLog> getLogsForStudent(int studentId) throws SQLException {
        List<AcademicAuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM academic_audit_log WHERE student_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<AcademicAuditLog> getRecentLogs(int limit) throws SQLException {
        List<AcademicAuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM academic_audit_log ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private AcademicAuditLog mapRow(ResultSet rs) throws SQLException {
        AcademicAuditLog log = new AcademicAuditLog();
        log.setId(rs.getInt("id"));
        log.setEntityType(rs.getString("entity_type"));
        
        int entityId = rs.getInt("entity_id");
        if (!rs.wasNull()) log.setEntityId(entityId);

        int studentId = rs.getInt("student_id");
        if (!rs.wasNull()) log.setStudentId(studentId);

        int courseId = rs.getInt("course_id");
        if (!rs.wasNull()) log.setCourseId(courseId);

        log.setAcademicYear(rs.getString("academic_year"));
        log.setSemester(rs.getInt("semester"));
        log.setAction(rs.getString("action"));
        log.setPreviousValue(rs.getString("previous_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setReason(rs.getString("reason"));

        int authBy = rs.getInt("authorized_by");
        if (!rs.wasNull()) log.setAuthorizedBy(authBy);

        log.setAuthorizedUser(rs.getString("authorized_user"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }
}
