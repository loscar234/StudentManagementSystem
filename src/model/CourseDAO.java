package model;

import db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CourseDAO – Data Access Object for {@link Course} entities.
 */
public class CourseDAO {

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    public void insert(Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_code, title, description, credit_hours, department, level, semester) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getCourseCode());
            ps.setString(2, course.getTitle());
            ps.setString(3, course.getDescription());
            ps.setInt(4, course.getCreditHours());
            ps.setString(5, course.getDepartment());
            ps.setInt(6, course.getLevel());
            ps.setInt(7, course.getSemester());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) course.setId(keys.getInt(1));
            }
        }
    }

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    public Course findById(int id) throws SQLException {
        String sql = "SELECT * FROM courses WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Course findByCode(String courseCode) throws SQLException {
        String sql = "SELECT * FROM courses WHERE course_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Course> findAll() throws SQLException {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT * FROM courses ORDER BY course_code";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Searches courses by code or title (case-insensitive partial match).
     *
     * @param keyword search keyword
     * @return matching courses
     * @throws SQLException on database error
     */
    public List<Course> search(String keyword) throws SQLException {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE course_code LIKE ? OR title LIKE ? ORDER BY course_code";
        String kw = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw); ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    public void update(Course course) throws SQLException {
        String sql = "UPDATE courses SET course_code=?, title=?, description=?, credit_hours=?, "
                   + "department=?, level=?, semester=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getCourseCode()); ps.setString(2, course.getTitle());
            ps.setString(3, course.getDescription()); ps.setInt(4, course.getCreditHours());
            ps.setString(5, course.getDepartment()); ps.setInt(6, course.getLevel());
            ps.setInt(7, course.getSemester()); ps.setInt(8, course.getId());
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    public void delete(int courseId) throws SQLException {
        String sql = "DELETE FROM courses WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Course mapRow(ResultSet rs) throws SQLException {
        return new Course(
            rs.getInt("id"),
            rs.getString("course_code"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("credit_hours"),
            rs.getString("department"),
            rs.getInt("level"),
            rs.getInt("semester")
        );
    }
}
