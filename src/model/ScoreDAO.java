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
     *
     * @param score the score to save
     * @throws SQLException on database error
     */
    public void upsert(Score score) throws SQLException {
        String sql = "INSERT INTO scores (student_id, course_id, academic_year, semester, cat_score, exam_score, remarks, entered_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE cat_score=VALUES(cat_score), exam_score=VALUES(exam_score), "
                   + "remarks=VALUES(remarks), entered_by=VALUES(entered_by), updated_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, score.getStudentId());
            ps.setInt(2, score.getCourseId());
            ps.setString(3, score.getAcademicYear());
            ps.setInt(4, score.getSemester());
            ps.setDouble(5, score.getCatScore());
            ps.setDouble(6, score.getExamScore());
            ps.setString(7, score.getRemarks());
            ps.setInt(8, score.getEnteredBy());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) score.setId(keys.getInt(1));
        }
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
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, academicYear);
            ps.setInt(3, semester);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Score s = mapRow(rs);
                s.setCourseCode(rs.getString("course_code"));
                s.setCourseTitle(rs.getString("course_title"));
                list.add(s);
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
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, regNumber);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Score s = mapRow(rs);
                s.setCourseCode(rs.getString("course_code"));
                s.setCourseTitle(rs.getString("course_title"));
                list.add(s);
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    public void delete(int scoreId) throws SQLException {
        String sql = "DELETE FROM scores WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, scoreId);
            ps.executeUpdate();
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
        return s;
    }
}
