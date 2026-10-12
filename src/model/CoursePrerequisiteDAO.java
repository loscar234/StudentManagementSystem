package model;

import db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CoursePrerequisiteDAO - Validates and manages prerequisite academic requirements.
 */
public class CoursePrerequisiteDAO {

    public List<CoursePrerequisite> getPrerequisitesForCourse(int courseId) throws SQLException {
        List<CoursePrerequisite> list = new ArrayList<>();
        String sql = "SELECT cp.*, c.course_code AS prereq_code, c.title AS prereq_title " +
                "FROM course_prerequisites cp " +
                "JOIN courses c ON c.id = cp.prerequisite_course_id " +
                "WHERE cp.course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CoursePrerequisite cp = new CoursePrerequisite();
                    cp.setId(rs.getInt("id"));
                    cp.setCourseId(rs.getInt("course_id"));
                    cp.setPrerequisiteCourseId(rs.getInt("prerequisite_course_id"));
                    cp.setPrerequisiteCourseCode(rs.getString("prereq_code"));
                    cp.setPrerequisiteCourseTitle(rs.getString("prereq_title"));
                    cp.setMinGrade(rs.getString("min_grade"));
                    list.add(cp);
                }
            }
        }
        return list;
    }

    /**
     * Checks if a student has passed all prerequisites for a given course.
     * Considers passing if student achieved a grade >= minGrade (or not 'F').
     *
     * @return List of descriptions for unmet prerequisites (empty if all prerequisites are satisfied).
     */
    public List<String> getUnmetPrerequisites(int studentId, int courseId) throws SQLException {
        List<CoursePrerequisite> prerequisites = getPrerequisitesForCourse(courseId);
        List<String> unmet = new ArrayList<>();

        if (prerequisites.isEmpty()) {
            return unmet;
        }

        String scoreSql = "SELECT grade, total_score FROM scores WHERE student_id = ? AND course_id = ? ORDER BY total_score DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(scoreSql)) {
            for (CoursePrerequisite req : prerequisites) {
                ps.setInt(1, studentId);
                ps.setInt(2, req.getPrerequisiteCourseId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Student has not completed the course
                        unmet.add(req.getPrerequisiteCourseCode() + " - " + req.getPrerequisiteCourseTitle() + " (Not yet taken)");
                    } else {
                        String grade = rs.getString("grade");
                        if (grade == null || "F".equalsIgnoreCase(grade)) {
                            unmet.add(req.getPrerequisiteCourseCode() + " - " + req.getPrerequisiteCourseTitle() + " (Failed with grade: " + grade + ")");
                        }
                    }
                }
            }
        }
        return unmet;
    }

    public boolean addPrerequisite(int courseId, int prerequisiteCourseId, String minGrade) throws SQLException {
        String sql = "INSERT INTO course_prerequisites (course_id, prerequisite_course_id, min_grade) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, prerequisiteCourseId);
            ps.setString(3, minGrade != null ? minGrade : "D");
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removePrerequisite(int courseId, int prerequisiteCourseId) throws SQLException {
        String sql = "DELETE FROM course_prerequisites WHERE course_id = ? AND prerequisite_course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, prerequisiteCourseId);
            return ps.executeUpdate() > 0;
        }
    }
}
