package controller;

import model.*;
import util.FileStorageManager;
import util.InputValidator;

import java.sql.SQLException;
import java.util.List;

/**
 * CourseController – Handles all business logic related to courses.
 */
public class CourseController {

    private final CourseDAO    courseDAO;
    private final StudentDAO   studentDAO;
    private final LecturerDAO  lecturerDAO;

    public CourseController() {
        this.courseDAO   = new CourseDAO();
        this.studentDAO  = new StudentDAO();
        this.lecturerDAO = new LecturerDAO();
    }

    // ---------------------------------------------------------------
    // Course CRUD
    // ---------------------------------------------------------------

    public Course addCourse(String courseCode, String title, String description,
                            int creditHours, String department, int level, int semester)
            throws SQLException {

        InputValidator.validateCourseCode(courseCode);
        InputValidator.validateRequired(title, "Course title");
        InputValidator.validateRequired(department, "Department");
        InputValidator.validateSemester(semester);

        if (creditHours < 1 || creditHours > 6) {
            throw new IllegalArgumentException("Credit hours must be between 1 and 6.");
        }

        // Check duplicate course code
        Course existing = courseDAO.findByCode(courseCode.trim().toUpperCase());
        if (existing != null) {
            throw new IllegalArgumentException("Course code already exists: " + courseCode);
        }

        Course course = new Course(0, courseCode.trim().toUpperCase(), title.trim(),
                description, creditHours, department.trim(), level, semester);
        courseDAO.insert(course);
        FileStorageManager.log("New course added: " + course.getCourseCode());
        return course;
    }

    public void updateCourse(Course course) throws SQLException {
        InputValidator.validateCourseCode(course.getCourseCode());
        InputValidator.validateRequired(course.getTitle(), "Course title");
        courseDAO.update(course);
        FileStorageManager.log("Course updated: " + course.getCourseCode());
    }

    public void deleteCourse(int courseId) throws SQLException {
        Course c = courseDAO.findById(courseId);
        if (c == null) throw new IllegalArgumentException("Course not found: ID " + courseId);
        courseDAO.delete(courseId);
        FileStorageManager.log("Course deleted: " + c.getCourseCode());
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public Course getCourseById(int id) throws SQLException {
        return courseDAO.findById(id);
    }

    public Course getCourseByCode(String code) throws SQLException {
        return courseDAO.findByCode(code);
    }

    public List<Course> getAllCourses() throws SQLException {
        return courseDAO.findAll();
    }

    /**
     * Searches courses by code or title.
     *
     * @param keyword search keyword
     * @return matching courses
     * @throws SQLException on database error
     */
    public List<Course> searchCourses(String keyword) throws SQLException {
        InputValidator.validateRequired(keyword, "Search keyword");
        return courseDAO.search(keyword.trim());
    }

    // ---------------------------------------------------------------
    // Allocation
    // ---------------------------------------------------------------

    /**
     * Allocates a course to a student (enforces 5-course limit via stored procedure).
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param academicYear academic year
     * @param semester     semester
     * @throws SQLException on database error or business rule violation
     */
    public void allocateCourseToStudent(int studentId, int courseId,
                                        String academicYear, int semester)
            throws SQLException {
        studentDAO.registerCourse(studentId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " allocated to student ID " + studentId);
    }

    /**
     * Allocates a course to a lecturer.
     *
     * @param lecturerId   lecturer primary key
     * @param courseId     course primary key
     * @param academicYear academic year
     * @param semester     semester
     * @throws SQLException on database error
     */
    public void allocateCourseToLecturer(int lecturerId, int courseId,
                                          String academicYear, int semester)
            throws SQLException {
        lecturerDAO.assignCourse(lecturerId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " allocated to lecturer ID " + lecturerId);
    }
}
