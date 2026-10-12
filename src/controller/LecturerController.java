package controller;

import model.*;
import util.FileStorageManager;
import util.InputValidator;
import util.SecurityContext;
import util.SessionManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * LecturerController – Handles all business logic related to lecturers.
 */
public class LecturerController {

    private final LecturerDAO lecturerDAO;

    public LecturerController() {
        this.lecturerDAO = new LecturerDAO();
    }

    // ---------------------------------------------------------------
    // Lecturer CRUD
    // ---------------------------------------------------------------

    public Lecturer addLecturer(String firstName, String lastName, String email,
                                String phone, String gender, LocalDate dateOfBirth,
                                String address, String staffId, String department,
                                String qualification, String specialization, LocalDate hireDate)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        InputValidator.validateName(firstName, "First name");
        InputValidator.validateName(lastName, "Last name");
        InputValidator.validateEmail(email);
        InputValidator.validatePhone(phone);
        InputValidator.validateRequired(gender, "Gender");
        InputValidator.validateStaffId(staffId);
        InputValidator.validateRequired(department, "Department");

        if (hireDate == null) throw new IllegalArgumentException("Hire date is required.");

        // Check duplicate staff ID
        Lecturer existing = lecturerDAO.findByStaffId(staffId.trim().toUpperCase());
        if (existing != null) throw new IllegalArgumentException("Staff ID already exists: " + staffId);

        Lecturer lecturer = new Lecturer(0, firstName.trim(), lastName.trim(),
                email.trim().toLowerCase(), phone.trim(), gender, dateOfBirth, address,
                staffId.trim().toUpperCase(), department.trim(), qualification, specialization,
                hireDate, "Active");

        lecturerDAO.insert(lecturer);
        FileStorageManager.log("New lecturer added: " + lecturer.getStaffId());
        return lecturer;
    }

    public void updateLecturer(Lecturer lecturer) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        InputValidator.validateName(lecturer.getFirstName(), "First name");
        InputValidator.validateName(lecturer.getLastName(), "Last name");
        InputValidator.validateEmail(lecturer.getEmail());
        InputValidator.validateStaffId(lecturer.getStaffId());
        InputValidator.validateRequired(lecturer.getDepartment(), "Department");

        lecturerDAO.update(lecturer);
        FileStorageManager.log("Lecturer updated: " + lecturer.getStaffId());
    }

    public void deleteLecturer(int lecturerId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        Lecturer l = lecturerDAO.findById(lecturerId);
        if (l == null) throw new IllegalArgumentException("Lecturer not found: ID " + lecturerId);
        lecturerDAO.delete(lecturerId);
        FileStorageManager.log("Lecturer deleted: " + l.getStaffId());
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public Lecturer getLecturerById(int id) throws SQLException {
        SecurityContext.requireAuthenticated();
        return lecturerDAO.findById(id);
    }

    public Lecturer getLecturerByStaffId(String staffId) throws SQLException {
        SecurityContext.requireAuthenticated();
        return lecturerDAO.findByStaffId(staffId);
    }

    public List<Lecturer> getAllLecturers() throws SQLException {
        SecurityContext.requireAuthenticated();
        return lecturerDAO.findAll();
    }

    public List<Lecturer> searchLecturers(String keyword) throws SQLException {
        SecurityContext.requireAuthenticated();
        InputValidator.validateRequired(keyword, "Search keyword");
        return lecturerDAO.search(keyword.trim());
    }

    // ---------------------------------------------------------------
    // Course assignment
    // ---------------------------------------------------------------

    public void assignCourse(int lecturerId, int courseId, String academicYear, int semester)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);
        lecturerDAO.assignCourse(lecturerId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " assigned to lecturer ID " + lecturerId);
    }

    public List<Course> getAssignedCourses(int lecturerId, String academicYear, int semester)
            throws SQLException {
        SecurityContext.requireLecturerOwnershipOrAdmin(lecturerId);
        return lecturerDAO.getAssignedCourses(lecturerId, academicYear, semester);
    }

    /**
     * Searches assigned courses for a lecturer by keyword.
     *
     * @param lecturerId   lecturer primary key
     * @param academicYear academic year
     * @param semester     semester
     * @param keyword      search keyword
     * @return matching courses
     * @throws SQLException on database error
     */
    public List<Course> searchAssignedCourses(int lecturerId, String academicYear,
                                               int semester, String keyword)
            throws SQLException {
        SecurityContext.requireLecturerOwnershipOrAdmin(lecturerId);
        List<Course> all = lecturerDAO.getAssignedCourses(lecturerId, academicYear, semester);
        String kw = keyword.toLowerCase();
        return all.stream()
                  .filter(c -> c.getCourseCode().toLowerCase().contains(kw)
                            || c.getTitle().toLowerCase().contains(kw))
                  .toList();
    }
}
