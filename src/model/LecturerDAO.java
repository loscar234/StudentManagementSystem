package model;

import db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * LecturerDAO – Data Access Object for {@link Lecturer} entities.
 */
public class LecturerDAO {

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    public void insert(Lecturer lecturer) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String personSql = "INSERT INTO persons (first_name, last_name, email, phone, gender, date_of_birth, address) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                int personId;
                try (PreparedStatement ps = conn.prepareStatement(personSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, lecturer.getFirstName());
                    ps.setString(2, lecturer.getLastName());
                    ps.setString(3, lecturer.getEmail());
                    ps.setString(4, lecturer.getPhone());
                    ps.setString(5, lecturer.getGender());
                    ps.setDate(6, lecturer.getDateOfBirth() != null ? Date.valueOf(lecturer.getDateOfBirth()) : null);
                    ps.setString(7, lecturer.getAddress());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        personId = keys.getInt(1);
                    }
                }

                String lecSql = "INSERT INTO lecturers (person_id, staff_id, department, qualification, specialization, hire_date, status) "
                              + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(lecSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, personId);
                    ps.setString(2, lecturer.getStaffId());
                    ps.setString(3, lecturer.getDepartment());
                    ps.setString(4, lecturer.getQualification());
                    ps.setString(5, lecturer.getSpecialization());
                    ps.setDate(6, Date.valueOf(lecturer.getHireDate()));
                    ps.setString(7, lecturer.getStatus() != null ? lecturer.getStatus() : "Active");
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        lecturer.setId(keys.getInt(1));
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    public Lecturer findById(int id) throws SQLException {
        String sql = buildBaseQuery() + " WHERE l.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Lecturer findByStaffId(String staffId) throws SQLException {
        String sql = buildBaseQuery() + " WHERE l.staff_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Lecturer> findAll() throws SQLException {
        List<Lecturer> list = new ArrayList<>();
        String sql = buildBaseQuery() + " ORDER BY l.staff_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Lecturer> search(String keyword) throws SQLException {
        List<Lecturer> list = new ArrayList<>();
        String sql = buildBaseQuery()
                   + " WHERE l.staff_id LIKE ? OR p.first_name LIKE ? OR p.last_name LIKE ? OR l.department LIKE ?"
                   + " ORDER BY l.staff_id";
        String kw = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw); ps.setString(2, kw); ps.setString(3, kw); ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    public void update(Lecturer lecturer) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int personId = getPersonId(conn, lecturer.getId());

                String personSql = "UPDATE persons SET first_name=?, last_name=?, email=?, phone=?, gender=?, "
                                 + "date_of_birth=?, address=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(personSql)) {
                    ps.setString(1, lecturer.getFirstName()); ps.setString(2, lecturer.getLastName());
                    ps.setString(3, lecturer.getEmail());     ps.setString(4, lecturer.getPhone());
                    ps.setString(5, lecturer.getGender());
                    ps.setDate(6, lecturer.getDateOfBirth() != null ? Date.valueOf(lecturer.getDateOfBirth()) : null);
                    ps.setString(7, lecturer.getAddress());   ps.setInt(8, personId);
                    ps.executeUpdate();
                }

                String lecSql = "UPDATE lecturers SET staff_id=?, department=?, qualification=?, specialization=?, "
                              + "hire_date=?, status=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(lecSql)) {
                    ps.setString(1, lecturer.getStaffId());       ps.setString(2, lecturer.getDepartment());
                    ps.setString(3, lecturer.getQualification()); ps.setString(4, lecturer.getSpecialization());
                    ps.setDate(5, Date.valueOf(lecturer.getHireDate()));
                    ps.setString(6, lecturer.getStatus());        ps.setInt(7, lecturer.getId());
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    public void delete(int lecturerId) throws SQLException {
        String sql = "DELETE FROM lecturers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // Course assignment
    // ---------------------------------------------------------------

    public void assignCourse(int lecturerId, int courseId, String academicYear, int semester)
            throws SQLException {
        String sql = "INSERT IGNORE INTO lecturer_courses (lecturer_id, course_id, academic_year, semester, assigned_date) "
                   + "VALUES (?, ?, ?, ?, CURDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId); ps.setInt(2, courseId);
            ps.setString(3, academicYear); ps.setInt(4, semester);
            ps.executeUpdate();
        }
    }

    public List<Course> getAssignedCourses(int lecturerId, String academicYear, int semester)
            throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.* FROM lecturer_courses lc "
                   + "JOIN courses c ON c.id = lc.course_id "
                   + "WHERE lc.lecturer_id = ? AND lc.academic_year = ? AND lc.semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId); ps.setString(2, academicYear); ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(
                        rs.getInt("id"), rs.getString("course_code"), rs.getString("title"),
                        rs.getString("description"), rs.getInt("credit_hours"),
                        rs.getString("department"), rs.getInt("level"), rs.getInt("semester")
                    ));
                }
            }
        }
        return courses;
    }

    /**
     * Checks if a specific course is assigned to a lecturer in the given academic year/semester.
     */
    public boolean isCourseAssignedToLecturer(int lecturerId, int courseId, String academicYear, int semester) throws SQLException {
        String sql = "SELECT 1 FROM lecturer_courses WHERE lecturer_id = ? AND course_id = ? AND academic_year = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            ps.setInt(2, courseId);
            ps.setString(3, academicYear);
            ps.setInt(4, semester);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Calculates the sum of credit hours of all courses assigned to a lecturer in the specified term.
     */
    public int getTotalAssignedCreditHours(int lecturerId, String academicYear, int semester) throws SQLException {
        String sql = "SELECT COALESCE(SUM(c.credit_hours), 0) FROM lecturer_courses lc " +
                     "JOIN courses c ON c.id = lc.course_id " +
                     "WHERE lc.lecturer_id = ? AND lc.academic_year = ? AND lc.semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            ps.setString(2, academicYear);
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private String buildBaseQuery() {
        return "SELECT l.id, l.staff_id, l.department, l.qualification, l.specialization, l.hire_date, l.status, "
             + "p.first_name, p.last_name, p.email, p.phone, p.gender, p.date_of_birth, p.address "
             + "FROM lecturers l JOIN persons p ON l.person_id = p.id";
    }

    private Lecturer mapRow(ResultSet rs) throws SQLException {
        Lecturer l = new Lecturer();
        l.setId(rs.getInt("id"));
        l.setStaffId(rs.getString("staff_id"));
        l.setDepartment(rs.getString("department"));
        l.setQualification(rs.getString("qualification"));
        l.setSpecialization(rs.getString("specialization"));
        Date hd = rs.getDate("hire_date");
        if (hd != null) l.setHireDate(hd.toLocalDate());
        l.setStatus(rs.getString("status"));
        l.setFirstName(rs.getString("first_name"));
        l.setLastName(rs.getString("last_name"));
        l.setEmail(rs.getString("email"));
        l.setPhone(rs.getString("phone"));
        l.setGender(rs.getString("gender"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) l.setDateOfBirth(dob.toLocalDate());
        l.setAddress(rs.getString("address"));
        return l;
    }

    private int getPersonId(Connection conn, int lecturerId) throws SQLException {
        String sql = "SELECT person_id FROM lecturers WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("person_id");
            }
        }
        throw new SQLException("Lecturer not found: " + lecturerId);
    }
}
