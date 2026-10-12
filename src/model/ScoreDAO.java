package model;

import db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ScoreDAO – Data Access Object for {@link Score} entities.
 */
public class ScoreDAO {

    // ---------------------------------------------------------------
    // CREATE / UPSERT
    // ---------------------------------------------------------------

    /**
     * Inserts or updates a score record (upsert semantics).
     * Prevents modifying scores that have already been finalized.
     *
     * @param score the score to save
     * @throws SQLException on database error
     */
    public void upsert(Score score) throws SQLException {
        Score existing = findScore(score.getStudentId(), score.getCourseId(), score.getAcademicYear(), score.getSemester());
        if (existing != null && "FINALIZED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Scores for this course have been finalized. " +
                    "Direct modifications are locked; an authorized score correction is required.");
        }

        String sql = "INSERT INTO scores (student_id, course_id, academic_year, semester, cat_score, exam_score, remarks, entered_by, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE cat_score=VALUES(cat_score), exam_score=VALUES(exam_score), "
                   + "remarks=VALUES(remarks), entered_by=VALUES(entered_by), updated_at=CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, score.getStudentId());
            ps.setInt(2, score.getCourseId());
            ps.setString(3, score.getAcademicYear());
            ps.setInt(4, score.getSemester());
            ps.setDouble(5, score.getCatScore());
            ps.setDouble(6, score.getExamScore());
            ps.setString(7, score.getRemarks());
            ps.setInt(8, score.getEnteredBy());
            ps.setString(9, score.getStatus() != null ? score.getStatus() : "DRAFT");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) score.setId(keys.getInt(1));
            }
        }
    }

    public Score findById(int id) throws SQLException {
        String sql = "SELECT sc.*, c.course_code, c.title AS course_title " +
                     "FROM scores sc JOIN courses c ON c.id = sc.course_id " +
                     "WHERE sc.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Score s = mapRow(rs);
                    s.setCourseCode(rs.getString("course_code"));
                    s.setCourseTitle(rs.getString("course_title"));
                    return s;
                }
            }
        }
        return null;
    }

    public Score findScore(int studentId, int courseId, String academicYear, int semester) throws SQLException {
        String sql = "SELECT sc.*, c.course_code, c.title AS course_title " +
                     "FROM scores sc JOIN courses c ON c.id = sc.course_id " +
                     "WHERE sc.student_id = ? AND sc.course_id = ? AND sc.academic_year = ? AND sc.semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setString(3, academicYear);
            ps.setInt(4, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Score s = mapRow(rs);
                    s.setCourseCode(rs.getString("course_code"));
                    s.setCourseTitle(rs.getString("course_title"));
                    return s;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // READ – by student
    // ---------------------------------------------------------------

    /**
     * Returns all scores for a student in a given academic year/semester.
     *
     * @param studentId    student primary key
     * @param academicYear academic year string
     * @param semester     semester number
     * @return list of scores with course details
     * @throws SQLException on database error
     */
    public List<Score> findByStudent(int studentId, String academicYear, int semester)
            throws SQLException {
        List<Score> list = new ArrayList<>();
        String sql = "SELECT sc.*, c.course_code, c.title AS course_title "
                   + "FROM scores sc JOIN courses c ON c.id = sc.course_id "
                   + "WHERE sc.student_id = ? AND sc.academic_year = ? AND sc.semester = ? "
                   + "ORDER BY c.course_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, academicYear);
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Score s = mapRow(rs);
                    s.setCourseCode(rs.getString("course_code"));
                    s.setCourseTitle(rs.getString("course_title"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // READ – result slip data
    // ---------------------------------------------------------------

    /**
     * Returns the full result slip data for a student.
     *
     * @param regNumber student registration number
     * @return list of scores with course details
     * @throws SQLException on database error
     */
    public List<Score> getResultSlip(String regNumber) throws SQLException {
        List<Score> list = new ArrayList<>();
        String sql = "SELECT sc.*, c.course_code, c.title AS course_title "
                   + "FROM scores sc "
                   + "JOIN students st ON st.id = sc.student_id "
                   + "JOIN courses c   ON c.id  = sc.course_id "
                   + "WHERE st.reg_number = ? "
                   + "ORDER BY sc.academic_year, sc.semester, c.course_code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Score s = mapRow(rs);
                    s.setCourseCode(rs.getString("course_code"));
                    s.setCourseTitle(rs.getString("course_title"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    public void delete(int scoreId) throws SQLException {
        String sql = "DELETE FROM scores WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scoreId);
            ps.executeUpdate();
        }
    }

    /**
     * Finalizes scores for a given course and academic term.
     * Prevents further direct editing by lecturers.
     */
    public int finalizeScores(String academicYear, int semester, int courseId, int authorizedBy, String authorizedUser) throws SQLException {
        String updateSql = "UPDATE scores SET status = 'FINALIZED', finalized_at = CURRENT_TIMESTAMP, finalized_by = ? " +
                           "WHERE academic_year = ? AND semester = ? AND course_id = ? AND status != 'FINALIZED'";
        String auditSql = "INSERT INTO academic_audit_log (entity_type, course_id, academic_year, semester, action, " +
                "previous_value, new_value, reason, authorized_by, authorized_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updatedCount;
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, authorizedBy);
                    ps.setString(2, academicYear);
                    ps.setInt(3, semester);
                    ps.setInt(4, courseId);
                    updatedCount = ps.executeUpdate();
                }

                if (updatedCount > 0) {
                    try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                        psAudit.setString(1, "SCORE");
                        psAudit.setInt(2, courseId);
                        psAudit.setString(3, academicYear);
                        psAudit.setInt(4, semester);
                        psAudit.setString(5, "FINALIZED_COURSE_SCORES");
                        psAudit.setString(6, "DRAFT / SUBMITTED");
                        psAudit.setString(7, "FINALIZED (" + updatedCount + " scores)");
                        psAudit.setString(8, "Official finalization of course results");
                        psAudit.setInt(9, authorizedBy);
                        psAudit.setString(10, authorizedUser != null ? authorizedUser : "SYSTEM");
                        psAudit.executeUpdate();
                    }
                }

                conn.commit();
                return updatedCount;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Executes an authorized score correction on a finalized (or locked) score.
     * Preserves original values and writes an audit log entry with user, reason, and timestamp.
     */
    public boolean correctScore(int scoreId, double newCat, double newExam, String reason,
                                int authorizedBy, String authorizedUser) throws SQLException {
        Score existing = findById(scoreId);
        if (existing == null) {
            throw new IllegalArgumentException("Score record not found for ID: " + scoreId);
        }

        String prevVal = String.format("CAT=%.2f, Exam=%.2f, Total=%.2f, Grade=%s",
                existing.getCatScore(), existing.getExamScore(), existing.getTotalScore(), existing.getGrade());

        String updateSql = "UPDATE scores SET cat_score = ?, exam_score = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String auditSql = "INSERT INTO academic_audit_log (entity_type, entity_id, student_id, course_id, academic_year, semester, " +
                "action, previous_value, new_value, reason, authorized_by, authorized_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, newCat);
                    ps.setDouble(2, newExam);
                    ps.setInt(3, scoreId);
                    ps.executeUpdate();
                }

                String newVal = String.format("CAT=%.2f, Exam=%.2f, Total=%.2f",
                        newCat, newExam, (newCat + newExam));

                try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "SCORE");
                    psAudit.setInt(2, scoreId);
                    psAudit.setInt(3, existing.getStudentId());
                    psAudit.setInt(4, existing.getCourseId());
                    psAudit.setString(5, existing.getAcademicYear());
                    psAudit.setInt(6, existing.getSemester());
                    psAudit.setString(7, "SCORE_CORRECTION");
                    psAudit.setString(8, prevVal);
                    psAudit.setString(9, newVal);
                    psAudit.setString(10, reason != null && !reason.trim().isEmpty() ? reason.trim() : "Authorized score correction");
                    psAudit.setInt(11, authorizedBy);
                    psAudit.setString(12, authorizedUser != null ? authorizedUser : "SYSTEM");
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

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Score mapRow(ResultSet rs) throws SQLException {
        Score s = new Score();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getInt("student_id"));
        s.setCourseId(rs.getInt("course_id"));
        s.setAcademicYear(rs.getString("academic_year"));
        s.setSemester(rs.getInt("semester"));
        s.setCatScore(rs.getDouble("cat_score"));
        s.setExamScore(rs.getDouble("exam_score"));
        s.setRemarks(rs.getString("remarks"));
        s.setEnteredBy(rs.getInt("entered_by"));
        Timestamp ts = rs.getTimestamp("entered_at");
        if (ts != null) s.setEnteredAt(ts.toLocalDateTime());

        try {
            s.setStatus(rs.getString("status"));
            Timestamp fts = rs.getTimestamp("finalized_at");
            if (fts != null) s.setFinalizedAt(fts.toLocalDateTime());
            int fb = rs.getInt("finalized_by");
            if (!rs.wasNull()) s.setFinalizedBy(fb);
        } catch (SQLException ignored) {
            // In case columns were omitted in non-wildcard queries
        }

        return s;
    }
}
