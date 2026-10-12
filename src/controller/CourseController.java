package controller;

import model.*;
import util.FileStorageManager;
import util.InputValidator;
import util.SecurityContext;
import util.SessionManager;

import java.sql.SQLException;
import java.util.List;

/**
 * CourseController – Handles all business logic related to courses.
 */
public class CourseController {

    private final CourseDAO             courseDAO;
    private final StudentDAO            studentDAO;
    private final LecturerDAO           lecturerDAO;
    private final AcademicTermDAO       academicTermDAO;
    private final CoursePrerequisiteDAO prerequisiteDAO;

    public CourseController() {
        this.courseDAO       = new CourseDAO();
        this.studentDAO      = new StudentDAO();
        this.lecturerDAO     = new LecturerDAO();
        this.academicTermDAO = new AcademicTermDAO();
        this.prerequisiteDAO = new CoursePrerequisiteDAO();
    }

    // ---------------------------------------------------------------
    // Course CRUD
    // ---------------------------------------------------------------

    public Course addCourse(String courseCode, String title, String description,
                            int creditHours, String department, int level, int semester)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

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
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        InputValidator.validateCourseCode(course.getCourseCode());
        InputValidator.validateRequired(course.getTitle(), "Course title");
        courseDAO.update(course);
        FileStorageManager.log("Course updated: " + course.getCourseCode());
    }

    public void deleteCourse(int courseId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        Course c = courseDAO.findById(courseId);
        if (c == null) throw new IllegalArgumentException("Course not found: ID " + courseId);
        courseDAO.delete(courseId);
        FileStorageManager.log("Course deleted: " + c.getCourseCode());
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public Course getCourseById(int id) throws SQLException {
        SecurityContext.requireAuthenticated();
        return courseDAO.findById(id);
    }

    public Course getCourseByCode(String code) throws SQLException {
        SecurityContext.requireAuthenticated();
        return courseDAO.findByCode(code);
    }

    public List<Course> getAllCourses() throws SQLException {
        SecurityContext.requireAuthenticated();
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
        SecurityContext.requireAuthenticated();
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
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        Course course = courseDAO.findById(courseId);
        if (course == null) throw new IllegalArgumentException("Course not found: ID " + courseId);

        // 1. Enforce duplicate enrollment check in Java layer
        if (studentDAO.isEnrolled(studentId, courseId, academicYear, semester)) {
            throw new IllegalArgumentException(String.format(
                "Duplicate enrollment: Student '%s' is already registered for course '%s' in %s Semester %d.",
                student.getRegNumber(), course.getCourseCode(), academicYear, semester
            ));
        }

        // 2. Enforce configurable maximum student course limit
        int currentCount = studentDAO.getEnrolledCourseCount(studentId, academicYear, semester);
        int maxCourses = util.SystemConfig.getMaxStudentCoursesPerSemester();
        if (currentCount >= maxCourses) {
            throw new IllegalArgumentException(String.format(
                "Student '%s' has reached the maximum enrollment limit of %d courses for %s Semester %d.",
                student.getRegNumber(), maxCourses, academicYear, semester
            ));
        }

        // 3. Enforce academic term lifecycle and registration deadline
        AcademicTerm term = academicTermDAO.findByYearAndSemester(academicYear, semester);
        if (term != null) {
            if (term.isClosed()) {
                throw new IllegalStateException(String.format(
                    "Academic term %s Semester %d is currently %s. Standard enrollment is closed. Authorized administrative override required.",
                    academicYear, semester, term.getStatus()
                ));
            }
            if (java.time.LocalDate.now().isAfter(term.getRegistrationDeadline())) {
                throw new IllegalStateException(String.format(
                    "Standard registration deadline (%s) for %s Semester %d has passed. Late registration requires authorized administrative action.",
                    term.getRegistrationDeadline(), academicYear, semester
                ));
            }
        }

        // 4. Enforce configurable course prerequisites
        if (util.SystemConfig.isEnforcePrerequisites()) {
            List<String> unmet = prerequisiteDAO.getUnmetPrerequisites(studentId, courseId);
            if (!unmet.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                    "Cannot register course '%s'. Unmet prerequisite(s): %s",
                    course.getCourseCode(), String.join("; ", unmet)
                ));
            }
        }

        studentDAO.registerCourse(studentId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + course.getCourseCode() + " allocated to student ID " + studentId);
    }

    /**
     * Allocates a course to a student under an authorized administrative override.
     */
    public void allocateCourseToStudentWithException(int studentId, int courseId,
                                                    String academicYear, int semester, String reason)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A justification reason is mandatory for exception course allocation.");
        }

        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        Course course = courseDAO.findById(courseId);
        if (course == null) throw new IllegalArgumentException("Course not found: ID " + courseId);

        if (studentDAO.isEnrolled(studentId, courseId, academicYear, semester)) {
            throw new IllegalArgumentException(String.format(
                "Duplicate enrollment: Student '%s' is already registered for course '%s' in %s Semester %d.",
                student.getRegNumber(), course.getCourseCode(), academicYear, semester
            ));
        }

        int currentCount = studentDAO.getEnrolledCourseCount(studentId, academicYear, semester);
        int maxCourses = util.SystemConfig.getMaxStudentCoursesPerSemester();
        if (currentCount >= maxCourses) {
            throw new IllegalArgumentException(String.format(
                "Student '%s' has reached the maximum enrollment limit of %d courses for this semester.",
                student.getRegNumber(), maxCourses
            ));
        }

        SessionManager sm = SessionManager.getInstance();
        int authUserId = sm.getCurrentUserId() != null ? sm.getCurrentUserId() : 1;
        String authUsername = sm.getCurrentUsername() != null ? sm.getCurrentUsername() : "ADMIN";

        studentDAO.registerCourseException(studentId, courseId, academicYear, semester, authUserId, authUsername, reason.trim());
        FileStorageManager.log(String.format("EXCEPTION ALLOCATION: Course %s allocated to student ID %d by %s. Reason: %s",
                course.getCourseCode(), studentId, authUsername, reason.trim()));
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
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        Course course = courseDAO.findById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course not found: ID " + courseId);
        }

        // Configurable lecturer credit hours workload check
        int currentCredits = lecturerDAO.getTotalAssignedCreditHours(lecturerId, academicYear, semester);
        int maxAllowed = util.SystemConfig.getMaxLecturerCreditHours();
        if (currentCredits + course.getCreditHours() > maxAllowed) {
            throw new IllegalArgumentException(String.format(
                "Workload limit exceeded: Assigning course '%s' (%d credits) would bring lecturer's total workload to %d credits. Institutional limit is configured at %d credits.",
                course.getCourseCode(), course.getCreditHours(), (currentCredits + course.getCreditHours()), maxAllowed
            ));
        }

        lecturerDAO.assignCourse(lecturerId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + course.getCourseCode() + " allocated to lecturer ID " + lecturerId
                + " (" + academicYear + " Sem " + semester + ")");
    }
}
