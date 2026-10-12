package model;

import db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AcademicTermDAO - Data access for academic term lifecycles and governance.
 * Supports non-destructive term transitions (including term reopening with audit trail).
 */
public class AcademicTermDAO {

    public AcademicTerm getCurrentTerm() throws SQLException {
        String sql = "SELECT * FROM academic_terms WHERE is_current = TRUE LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        // Fallback: Return latest ACTIVE term if no term is flagged is_current
        String fallbackSql = "SELECT * FROM academic_terms WHERE status = 'ACTIVE' ORDER BY academic_year DESC, semester DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(fallbackSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public List<AcademicTerm> getAllTerms() throws SQLException {
        List<AcademicTerm> list = new ArrayList<>();
        String sql = "SELECT * FROM academic_terms ORDER BY academic_year DESC, semester DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public AcademicTerm findByYearAndSemester(String academicYear, int semester) throws SQLException {
        String sql = "SELECT * FROM academic_terms WHERE academic_year = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, academicYear);
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public AcademicTerm findById(int id) throws SQLException {
        String sql = "SELECT * FROM academic_terms WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates term status non-destructively, preserving original value and logging reason/authorizer.
     * Allows reopening a term (e.g. from CLOSED to ACTIVE) with documented justification.
     */
    public boolean updateStatus(int termId, String newStatus, int authorizedBy, String authorizedUser, String reason) throws SQLException {
        AcademicTerm term = findById(termId);
        if (term == null) {
            throw new IllegalArgumentException("Academic term not found for ID: " + termId);
        }

        String oldStatus = term.getStatus();
        if (oldStatus.equalsIgnoreCase(newStatus)) {
            return true; // No change needed
        }

        String updateSql = "UPDATE academic_terms SET status = ? WHERE id = ?";
        String auditSql = "INSERT INTO academic_audit_log (entity_type, entity_id, academic_year, semester, action, " +
                "previous_value, new_value, reason, authorized_by, authorized_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus.toUpperCase());
                    ps.setInt(2, termId);
                    ps.executeUpdate();
                }

                try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "TERM");
                    psAudit.setInt(2, termId);
                    psAudit.setString(3, term.getAcademicYear());
                    psAudit.setInt(4, term.getSemester());
                    psAudit.setString(5, "TERM_STATUS_CHANGE");
                    psAudit.setString(6, oldStatus);
                    psAudit.setString(7, newStatus.toUpperCase());
                    psAudit.setString(8, reason != null && !reason.trim().isEmpty() ? reason.trim() : "Term status updated");
                    psAudit.setInt(9, authorizedBy);
                    psAudit.setString(10, authorizedUser != null ? authorizedUser : "SYSTEM");
                    psAudit.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Sets the specified term as the current active term.
     */
    public boolean setCurrentTerm(int termId, int authorizedBy, String authorizedUser, String reason) throws SQLException {
        AcademicTerm term = findById(termId);
        if (term == null) {
            throw new IllegalArgumentException("Academic term not found for ID: " + termId);
        }

        String clearSql = "UPDATE academic_terms SET is_current = FALSE";
        String setSql = "UPDATE academic_terms SET is_current = TRUE, status = 'ACTIVE' WHERE id = ?";
        String auditSql = "INSERT INTO academic_audit_log (entity_type, entity_id, academic_year, semester, action, " +
                "previous_value, new_value, reason, authorized_by, authorized_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(clearSql);
                }
                try (PreparedStatement ps = conn.prepareStatement(setSql)) {
                    ps.setInt(1, termId);
                    ps.executeUpdate();
                }
                try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "TERM");
                    psAudit.setInt(2, termId);
                    psAudit.setString(3, term.getAcademicYear());
                    psAudit.setInt(4, term.getSemester());
                    psAudit.setString(5, "SET_CURRENT_TERM");
                    psAudit.setString(6, "Current changed");
                    psAudit.setString(7, "Current term set to ID " + termId);
                    psAudit.setString(8, reason != null ? reason : "Set active current academic term");
                    psAudit.setInt(9, authorizedBy);
                    psAudit.setString(10, authorizedUser != null ? authorizedUser : "SYSTEM");
                    psAudit.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private AcademicTerm mapRow(ResultSet rs) throws SQLException {
        AcademicTerm term = new AcademicTerm();
        term.setId(rs.getInt("id"));
        term.setAcademicYear(rs.getString("academic_year"));
        term.setSemester(rs.getInt("semester"));

        Date sDate = rs.getDate("start_date");
        if (sDate != null) term.setStartDate(sDate.toLocalDate());

        Date eDate = rs.getDate("end_date");
        if (eDate != null) term.setEndDate(eDate.toLocalDate());

        Date rsDate = rs.getDate("registration_start");
        if (rsDate != null) term.setRegistrationStart(rsDate.toLocalDate());

        Date rdDate = rs.getDate("registration_deadline");
        if (rdDate != null) term.setRegistrationDeadline(rdDate.toLocalDate());

        term.setStatus(rs.getString("status"));
        term.setCurrent(rs.getBoolean("is_current"));
        term.setCreatedAt(rs.getTimestamp("created_at"));
        term.setUpdatedAt(rs.getTimestamp("updated_at"));
        return term;
    }
}
