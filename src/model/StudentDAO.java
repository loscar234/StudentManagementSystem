package model;

import db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO – Data Access Object for {@link Student} entities.
 *
 * <p>Provides CRUD operations and search functionality against the
 * {@code students} and {@code persons} tables in MySQL.</p>
 */
public class StudentDAO {

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    /**
     * Inserts a new student (and their person record) into the database.
     *
     * @param student the student to insert
     * @throws SQLException on database error
     */
    public void insert(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert into persons
                String personSql = "INSERT INTO persons (first_name, last_name, email, phone, gender, date_of_birth, address) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                int personId;
                try (PreparedStatement ps = conn.prepareStatement(personSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, student.getFirstName());
                    ps.setString(2, student.getLastName());
                    ps.setString(3, student.getEmail());
                    ps.setString(4, student.getPhone());
                    ps.setString(5, student.getGender());
                    ps.setDate(6, student.getDateOfBirth() != null ? Date.valueOf(student.getDateOfBirth()) : null);
                    ps.setString(7, student.getAddress());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            personId = keys.getInt(1);
                        } else {
                            throw new SQLException("Failed to obtain person ID.");
                        }
                    }
                }

                // 2. Insert into students
                String studentSql = "INSERT INTO students (person_id, reg_number, programme, year_of_study, semester, enrollment_date, status) "
                                  + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(studentSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, personId);
                    ps.setString(2, student.getRegNumber());
                    ps.setString(3, student.getProgramme());
                    ps.setInt(4, student.getYearOfStudy());
                    ps.setInt(5, student.getSemester());
                    ps.setDate(6, Date.valueOf(student.getEnrollmentDate()));
                    ps.setString(7, student.getStatus() != null ? student.getStatus() : "Active");
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            student.setId(keys.getInt(1));
                        }
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
    // READ – find by ID
    // ---------------------------------------------------------------

    /**
     * Retrieves a student by their database ID.
     *
     * @param id student primary key
     * @return the {@link Student}, or null if not found
     * @throws SQLException on database error
     */
    public Student findById(int id) throws SQLException {
        String sql = buildBaseQuery() + " WHERE s.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // READ – find by registration number
    // ---------------------------------------------------------------

    /**
     * Retrieves a student by their registration number.
     *
     * @param regNumber the registration number
     * @return the {@link Student}, or null if not found
     * @throws SQLException on database error
     */
    public Student findByRegNumber(String regNumber) throws SQLException {
        String sql = buildBaseQuery() + " WHERE s.reg_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // READ – get all
    // ---------------------------------------------------------------

    /**
     * Returns all students in the database.
     *
     * @return list of all students
     * @throws SQLException on database error
     */
    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = buildBaseQuery() + " ORDER BY s.reg_number";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ---------------------------------------------------------------
    // READ – search
    // ---------------------------------------------------------------

    /**
     * Searches students by name, registration number, or programme.
     *
     * @param keyword the search keyword
     * @return matching students
     * @throws SQLException on database error
     */
    public List<Student> search(String keyword) throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = buildBaseQuery()
                   + " WHERE s.reg_number LIKE ? OR p.first_name LIKE ? OR p.last_name LIKE ? OR s.programme LIKE ?"
                   + " ORDER BY s.reg_number";
        String kw = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    /**
     * Updates an existing student's details in the database.
     *
     * @param student the student with updated values
     * @throws SQLException on database error
     */
    public void update(Student student) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get person_id first
                int personId = getPersonId(conn, student.getId());

                String personSql = "UPDATE persons SET first_name=?, last_name=?, email=?, phone=?, gender=?, "
                                 + "date_of_birth=?, address=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(personSql)) {
                    ps.setString(1, student.getFirstName());
                    ps.setString(2, student.getLastName());
                    ps.setString(3, student.getEmail());
                    ps.setString(4, student.getPhone());
                    ps.setString(5, student.getGender());
                    ps.setDate(6, student.getDateOfBirth() != null ? Date.valueOf(student.getDateOfBirth()) : null);
                    ps.setString(7, student.getAddress());
                    ps.setInt(8, personId);
                    ps.executeUpdate();
                }

                String studentSql = "UPDATE students SET reg_number=?, programme=?, year_of_study=?, semester=?, "
                                  + "enrollment_date=?, status=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(studentSql)) {
                    ps.setString(1, student.getRegNumber());
                    ps.setString(2, student.getProgramme());
                    ps.setInt(3, student.getYearOfStudy());
                    ps.setInt(4, student.getSemester());
                    ps.setDate(5, Date.valueOf(student.getEnrollmentDate()));
                    ps.setString(6, student.getStatus());
                    ps.setInt(7, student.getId());
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

    /**
     * Deletes a student and their person record from the database.
     *
     * @param studentId the student's primary key
     * @throws SQLException on database error
     */
    public void delete(int studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int personId = getPersonId(conn, studentId);
            String sql = "DELETE FROM persons WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, personId);
                ps.executeUpdate();
            }
        }
    }

    // ---------------------------------------------------------------
    // Course registration
    // ---------------------------------------------------------------

    /**
     * Registers a student for a course in the database.
     * Enforces the 5-course maximum via stored procedure.
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param academicYear e.g. "2024/2025"
     * @param semester     1 or 2
     * @throws SQLException on database error or business rule violation
     */
    public void registerCourse(int studentId, int courseId, String academicYear, int semester)
            throws SQLException {
        String sql = "CALL sp_register_student_course(?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, studentId);
            cs.setInt(2, courseId);
            cs.setString(3, academicYear);
            cs.setInt(4, semester);
            cs.execute();
        }
    }

    /**
     * Deregisters a course for a student by removing the record from student_courses.
     *
     * @param studentId student primary key
     * @param courseId  course primary key
     * @throws SQLException on database error
     */
    public void deregisterCourse(int studentId, int courseId) throws SQLException {
        String sql = "DELETE FROM student_courses WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        }
    }

    public void deregisterCourse(int studentId, int courseId, String academicYear, int semester) throws SQLException {
        String sql = "DELETE FROM student_courses WHERE student_id = ? AND course_id = ? AND academic_year = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setString(3, academicYear);
            ps.setInt(4, semester);
            ps.executeUpdate();
        }
    }

    /**
     * Checks if a student is already enrolled in a specific course for the given term.
     */
    public boolean isEnrolled(int studentId, int courseId, String academicYear, int semester) throws SQLException {
        String sql = "SELECT COUNT(*) FROM student_courses WHERE student_id = ? AND course_id = ? AND academic_year = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setString(3, academicYear);
            ps.setInt(4, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Returns total courses enrolled by the student in the specified term.
     */
    public int getEnrolledCourseCount(int studentId, String academicYear, int semester) throws SQLException {
        String sql = "SELECT COUNT(*) FROM student_courses WHERE student_id = ? AND academic_year = ? AND semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
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

    /**
     * Registers a student for a course under an authorized exception (e.g. late registration or closed term override).
     * Invokes sp_register_student_course_exception and logs to academic_audit_log.
     */
    public void registerCourseException(int studentId, int courseId, String academicYear, int semester,
                                        int authorizedBy, String authorizedUser, String reason) throws SQLException {
        String sql = "CALL sp_register_student_course_exception(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, studentId);
            cs.setInt(2, courseId);
            cs.setString(3, academicYear);
            cs.setInt(4, semester);
            cs.setInt(5, authorizedBy);
            cs.setString(6, authorizedUser);
            cs.setString(7, reason);
            cs.execute();
        }
    }

    /**
     * Deregisters a course for a student under an authorized administrative action.
     * Removes the enrollment and creates an immutable entry in academic_audit_log.
     */
    public void deregisterCourseWithException(int studentId, int courseId, String academicYear, int semester,
                                              int authorizedBy, String authorizedUser, String reason) throws SQLException {
        String deleteSql = "DELETE FROM student_courses WHERE student_id = ? AND course_id = ? AND academic_year = ? AND semester = ?";
        String auditSql = "INSERT INTO academic_audit_log (entity_type, student_id, course_id, academic_year, semester, " +
                "action, previous_value, new_value, reason, authorized_by, authorized_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, courseId);
                    ps.setString(3, academicYear);
                    ps.setInt(4, semester);
                    ps.executeUpdate();
                }

                try (PreparedStatement psAudit = conn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "ENROLLMENT");
                    psAudit.setInt(2, studentId);
                    psAudit.setInt(3, courseId);
                    psAudit.setString(4, academicYear);
                    psAudit.setInt(5, semester);
                    psAudit.setString(6, "AUTHORIZED_COURSE_DROP");
                    psAudit.setString(7, "Enrolled course_id=" + courseId);
                    psAudit.setString(8, "DROPPED");
                    psAudit.setString(9, reason != null && !reason.trim().isEmpty() ? reason.trim() : "Authorized course drop");
                    psAudit.setInt(10, authorizedBy);
                    psAudit.setString(11, authorizedUser != null ? authorizedUser : "SYSTEM");
                    psAudit.executeUpdate();
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

    /**
     * Returns all courses registered by a student in a given academic year/semester.
     *
     * @param studentId    student primary key
     * @param academicYear academic year string
     * @param semester     semester number
     * @return list of enrolled courses
     * @throws SQLException on database error
     */
    public List<Course> getEnrolledCourses(int studentId, String academicYear, int semester)
            throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.* FROM student_courses sc "
                   + "JOIN courses c ON c.id = sc.course_id "
                   + "WHERE sc.student_id = ? AND sc.academic_year = ? AND sc.semester = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, academicYear);
            ps.setInt(3, semester);
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

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private String buildBaseQuery() {
        return "SELECT s.id, s.reg_number, s.programme, s.year_of_study, s.semester, "
             + "s.enrollment_date, s.status, "
             + "p.first_name, p.last_name, p.email, p.phone, p.gender, p.date_of_birth, p.address "
             + "FROM students s JOIN persons p ON s.person_id = p.id";
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setRegNumber(rs.getString("reg_number"));
        s.setProgramme(rs.getString("programme"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSemester(rs.getInt("semester"));
        Date ed = rs.getDate("enrollment_date");
        if (ed != null) s.setEnrollmentDate(ed.toLocalDate());
        s.setStatus(rs.getString("status"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setGender(rs.getString("gender"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) s.setDateOfBirth(dob.toLocalDate());
        s.setAddress(rs.getString("address"));
        return s;
    }

    private int getPersonId(Connection conn, int studentId) throws SQLException {
        String sql = "SELECT person_id FROM students WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("person_id");
            }
        }
        throw new SQLException("Student not found: " + studentId);
    }
}
