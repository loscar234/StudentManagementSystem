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
 * StudentController – Handles all business logic related to students.
 *
 * <p>Acts as the intermediary between the View layer (GUI) and the
 * Model layer (DAO classes). Performs input validation before
 * delegating persistence to {@link StudentDAO} and {@link ScoreDAO},
 * and mirrors writes to the file storage system.</p>
 */
public class StudentController {

    private final StudentDAO            studentDAO;
    private final ScoreDAO              scoreDAO;
    private final CourseDAO             courseDAO;
    private final LecturerDAO           lecturerDAO;
    private final AcademicTermDAO       academicTermDAO;
    private final CoursePrerequisiteDAO prerequisiteDAO;
    private final AcademicAuditDAO      auditDAO;

    public StudentController() {
        this.studentDAO      = new StudentDAO();
        this.scoreDAO        = new ScoreDAO();
        this.courseDAO       = new CourseDAO();
        this.lecturerDAO     = new LecturerDAO();
        this.academicTermDAO = new AcademicTermDAO();
        this.prerequisiteDAO = new CoursePrerequisiteDAO();
        this.auditDAO        = new AcademicAuditDAO();
    }

    // ---------------------------------------------------------------
    // Student CRUD
    // ---------------------------------------------------------------

    /**
     * Registers a new student after validating all input fields.
     *
     * @param firstName      first name
     * @param lastName       last name
     * @param email          email address
     * @param phone          phone number
     * @param gender         gender
     * @param dateOfBirth    date of birth
     * @param address        residential address
     * @param regNumber      registration number
     * @param programme      academic programme
     * @param yearOfStudy    year of study (1–6)
     * @param semester       semester (1 or 2)
     * @param enrollmentDate date of enrollment
     * @return the newly created {@link Student}
     * @throws IllegalArgumentException if any input is invalid
     * @throws SQLException             on database error
     */
    public Student registerStudent(String firstName, String lastName, String email,
                                   String phone, String gender, LocalDate dateOfBirth,
                                   String address, String regNumber, String programme,
                                   int yearOfStudy, int semester, LocalDate enrollmentDate)
            throws SQLException {

        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        // Validate inputs
        InputValidator.validateName(firstName, "First name");
        InputValidator.validateName(lastName, "Last name");
        InputValidator.validateEmail(email);
        InputValidator.validatePhone(phone);
        InputValidator.validateRequired(gender, "Gender");
        InputValidator.validateRegNumber(regNumber);
        InputValidator.validateRequired(programme, "Programme");
        InputValidator.validateYearOfStudy(yearOfStudy);
        InputValidator.validateSemester(semester);


        if (enrollmentDate == null) {
            throw new IllegalArgumentException("Enrollment date is required.");
        }

        // Check duplicate registration number
        Student existing = studentDAO.findByRegNumber(regNumber.trim().toUpperCase());
        if (existing != null) {
            throw new IllegalArgumentException("Registration number already exists: " + regNumber);
        }

        Student student = new Student(0, firstName.trim(), lastName.trim(), email.trim().toLowerCase(),
                phone.trim(), gender, dateOfBirth, address, regNumber.trim().toUpperCase(),
                programme.trim(), yearOfStudy, semester, enrollmentDate, "Active");

        studentDAO.insert(student);
        FileStorageManager.saveStudent(student);
        FileStorageManager.log("New student registered: " + student.getRegNumber());

        return student;
    }

    /**
     * Updates an existing student's information.
     *
     * @param student the student with updated values
     * @throws IllegalArgumentException if any input is invalid
     * @throws SQLException             on database error
     */
    public void updateStudent(Student student) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        InputValidator.validateName(student.getFirstName(), "First name");
        InputValidator.validateName(student.getLastName(), "Last name");
        InputValidator.validateEmail(student.getEmail());
        InputValidator.validateRegNumber(student.getRegNumber());
        InputValidator.validateRequired(student.getProgramme(), "Programme");
        InputValidator.validateYearOfStudy(student.getYearOfStudy());
        InputValidator.validateSemester(student.getSemester());

        studentDAO.update(student);
        FileStorageManager.log("Student updated: " + student.getRegNumber());
    }

    /**
     * Deletes a student by ID.
     *
     * @param studentId student primary key
     * @throws SQLException on database error
     */
    public void deleteStudent(int studentId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);

        Student s = studentDAO.findById(studentId);
        if (s == null) throw new IllegalArgumentException("Student not found: ID " + studentId);
        studentDAO.delete(studentId);
        FileStorageManager.log("Student deleted: " + s.getRegNumber());
    }

    // ---------------------------------------------------------------
    // Student queries
    // ---------------------------------------------------------------

    public Student getStudentById(int id) throws SQLException {
        SecurityContext.requireAuthenticated();
        return studentDAO.findById(id);
    }

    public Student getStudentByRegNumber(String regNumber) throws SQLException {
        SecurityContext.requireAuthenticated();
        return studentDAO.findByRegNumber(regNumber);
    }

    public List<Student> getAllStudents() throws SQLException {
        SecurityContext.requireAuthenticated();
        return studentDAO.findAll();
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        SecurityContext.requireAuthenticated();
        InputValidator.validateRequired(keyword, "Search keyword");
        return studentDAO.search(keyword.trim());
    }

    // ---------------------------------------------------------------
    // Course registration
    // ---------------------------------------------------------------

    /**
     * Registers a student for a course, enforcing term deadlines, duplicate checks, 
     * configurable prerequisites, and course limits.
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param academicYear academic year string
     * @param semester     semester number
     * @throws SQLException             on database error or business rule violation
     * @throws IllegalArgumentException if student or course not found, duplicate, or prerequisite unmet
     * @throws IllegalStateException    if term is closed or registration deadline has passed
     */
    public void registerCourse(int studentId, int courseId, String academicYear, int semester)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        // Verify student exists
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        // Verify course exists
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
            if (LocalDate.now().isAfter(term.getRegistrationDeadline())) {
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
        FileStorageManager.log("Course " + course.getCourseCode() + " registered for student " + student.getRegNumber()
                + " (" + academicYear + " Sem " + semester + ")");
    }

    /**
     * Returns all courses a student is enrolled in for a given period.
     *
     * @param studentId    student primary key
     * @param academicYear academic year
     * @param semester     semester number
     * @return list of enrolled courses
     * @throws SQLException on database error
     */
    public List<Course> getEnrolledCourses(int studentId, String academicYear, int semester)
            throws SQLException {
        SecurityContext.requireAuthenticated();
        return studentDAO.getEnrolledCourses(studentId, academicYear, semester);
    }

    /**
     * Deregisters a course for a student.
     *
     * @param studentId student primary key
     * @param courseId  course primary key
     * @throws SQLException on database error
     */
    public void deregisterCourse(int studentId, int courseId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);
        studentDAO.deregisterCourse(studentId, courseId);
        FileStorageManager.log("Course " + courseId + " deregistered for student " + student.getRegNumber());
    }

    /**
     * Deregisters a course for a student in a specific academic period.
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param academicYear academic year
     * @param semester     semester
     * @throws SQLException on database error
     */
    public void deregisterCourse(int studentId, int courseId, String academicYear, int semester) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);
        studentDAO.deregisterCourse(studentId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " deregistered for student " + student.getRegNumber()
                + " (" + academicYear + " Sem " + semester + ")");
    }

    /**
     * Registers a student for a course through an authorized administrative exception (e.g., late registration or term override).
     * Preserves an immutable audit trail capturing the authorizing user, justification reason, and timestamp.
     */
    public void registerCourseWithException(int studentId, int courseId, String academicYear, int semester, String reason)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("An authorized justification reason is mandatory for exception course registration.");
        }

        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        Course course = courseDAO.findById(courseId);
        if (course == null) throw new IllegalArgumentException("Course not found: ID " + courseId);

        // Enforce duplicate check even on exception
        if (studentDAO.isEnrolled(studentId, courseId, academicYear, semester)) {
            throw new IllegalArgumentException(String.format(
                "Duplicate enrollment: Student '%s' is already registered for course '%s' in %s Semester %d.",
                student.getRegNumber(), course.getCourseCode(), academicYear, semester
            ));
        }

        // Enforce max courses limit
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
        FileStorageManager.log(String.format("EXCEPTION ENROLLMENT: Course %s registered for %s by %s. Reason: %s",
                course.getCourseCode(), student.getRegNumber(), authUsername, reason.trim()));
    }

    /**
     * Deregisters a course for a student under an authorized administrative action with full audit trail logging.
     */
    public void deregisterCourseWithException(int studentId, int courseId, String academicYear, int semester, String reason)
            throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A justification reason is required for authorized course deregistration.");
        }

        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        Course course = courseDAO.findById(courseId);
        String courseCode = course != null ? course.getCourseCode() : ("ID:" + courseId);

        SessionManager sm = SessionManager.getInstance();
        int authUserId = sm.getCurrentUserId() != null ? sm.getCurrentUserId() : 1;
        String authUsername = sm.getCurrentUsername() != null ? sm.getCurrentUsername() : "ADMIN";

        studentDAO.deregisterCourseWithException(studentId, courseId, academicYear, semester, authUserId, authUsername, reason.trim());
        FileStorageManager.log(String.format("EXCEPTION DEREGISTRATION: Course %s dropped for %s by %s. Reason: %s",
                courseCode, student.getRegNumber(), authUsername, reason.trim()));
    }

    /**
     * Finalizes scores for a given course in an academic term, locking direct modifications.
     */
    public int finalizeCourseScores(String academicYear, int semester, int courseId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR, SessionManager.Role.APPROVER);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        SessionManager sm = SessionManager.getInstance();
        int authUserId = sm.getCurrentUserId() != null ? sm.getCurrentUserId() : 1;
        String authUsername = sm.getCurrentUsername() != null ? sm.getCurrentUsername() : "ADMIN";

        int count = scoreDAO.finalizeScores(academicYear, semester, courseId, authUserId, authUsername);
        FileStorageManager.log(String.format("Scores finalized for course %d in %s Sem %d by %s (%d records locked).",
                courseId, academicYear, semester, authUsername, count));
        return count;
    }

    /**
     * Executes an authorized score correction on a finalized or locked score record, recording the reason and authorizer.
     */
    public void correctScore(int scoreId, double newCat, double newExam, String reason) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR, SessionManager.Role.APPROVER);
        InputValidator.validateCatScore(newCat);
        InputValidator.validateExamScore(newExam);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A justification reason is strictly required for official score corrections.");
        }

        SessionManager sm = SessionManager.getInstance();
        int authUserId = sm.getCurrentUserId() != null ? sm.getCurrentUserId() : 1;
        String authUsername = sm.getCurrentUsername() != null ? sm.getCurrentUsername() : "ADMIN";

        scoreDAO.correctScore(scoreId, newCat, newExam, reason.trim(), authUserId, authUsername);
        FileStorageManager.log(String.format("SCORE CORRECTION: Score ID %d updated to CAT=%.1f, Exam=%.1f by %s. Reason: %s",
                scoreId, newCat, newExam, authUsername, reason.trim()));
    }

    /**
     * Returns academic audit history for a specific student.
     */
    public List<AcademicAuditLog> getAuditLogsForStudent(int studentId) throws SQLException {
        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.REGISTRAR);
        return auditDAO.getLogsForStudent(studentId);
    }

    /**
     * Returns all configured academic terms.
     */
    public List<AcademicTerm> getAllAcademicTerms() throws SQLException {
        SecurityContext.requireAuthenticated();
        return academicTermDAO.getAllTerms();
    }

    /**
     * Returns the currently active academic term.
     */
    public AcademicTerm getCurrentAcademicTerm() throws SQLException {
        SecurityContext.requireAuthenticated();
        return academicTermDAO.getCurrentTerm();
    }

    /**
     * Updates term status non-destructively (e.g. reopen closed term) with documented justification.
     */
    public void updateTermStatus(int termId, String newStatus, String reason) throws SQLException {
        SecurityContext.requireRole(SessionManager.Role.ADMIN);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A justification reason is required to alter term status.");
        }
        SessionManager sm = SessionManager.getInstance();
        int authUserId = sm.getCurrentUserId() != null ? sm.getCurrentUserId() : 1;
        String authUsername = sm.getCurrentUsername() != null ? sm.getCurrentUsername() : "ADMIN";
        academicTermDAO.updateStatus(termId, newStatus, authUserId, authUsername, reason.trim());
    }

    /**
     * Finds a course by its course code.
     *
     * @param courseCode course code (e.g. CS101)
     * @return the Course entity, or null if not found
     * @throws SQLException on database error
     */
    public Course getCourseByCode(String courseCode) throws SQLException {
        SecurityContext.requireAuthenticated();
        if (courseCode == null || courseCode.trim().isEmpty()) return null;
        return courseDAO.findByCode(courseCode.trim().toUpperCase());
    }

    // ---------------------------------------------------------------
    // Score management
    // ---------------------------------------------------------------

    /**
     * Enters or updates a student's score for a course using the authenticated user session.
     */
    public Score enterScore(int studentId, int courseId, String courseCode, String courseTitle,
                            String academicYear, int semester,
                            double catScore, double examScore)
            throws SQLException {
        Integer currentUserId = SessionManager.getInstance().getCurrentUserId();
        int enteredBy = (currentUserId != null) ? currentUserId : 1;
        return enterScore(studentId, courseId, courseCode, courseTitle, academicYear, semester, catScore, examScore, enteredBy);
    }

    public Score enterScore(int studentId, int courseId, String courseCode, String courseTitle,
                            String academicYear, int semester,
                            double catScore, double examScore, int enteredBy)
            throws SQLException {

        SecurityContext.requireAnyRole(SessionManager.Role.ADMIN, SessionManager.Role.LECTURER);

        // If the ID is 0, use the courseCode to find the real ID in the database
        if (courseId <= 0 && courseCode != null) {
            Course found = courseDAO.findByCode(courseCode.trim());
            if (found != null) {
                courseId = found.getId();
            } else {
                throw new IllegalArgumentException("Could not find course ID for: " + courseCode);
            }
        }

        // Server-side check: Lecturers can only enter scores for courses assigned to them
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentRole() == SessionManager.Role.LECTURER) {
            Integer sessionLecturerId = session.getLecturerId();
            if (sessionLecturerId == null) {
                throw new util.AccessDeniedException("Access Denied: Your account is not associated with an active lecturer profile.");
            }
            if (!lecturerDAO.isCourseAssignedToLecturer(sessionLecturerId, courseId, academicYear, semester)) {
                throw new util.AccessDeniedException(String.format(
                    "Access Denied: You are not assigned to teach course '%s' for %s Semester %d.",
                    (courseCode != null ? courseCode : "ID " + courseId), academicYear, semester
                ));
            }
            enteredBy = sessionLecturerId;
        } else if (enteredBy <= 0) {
            Integer currentUserId = session.getCurrentUserId();
            enteredBy = (currentUserId != null) ? currentUserId : 1;
        }

        InputValidator.validateCatScore(catScore);
        InputValidator.validateExamScore(examScore);
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        // This now uses the CORRECT courseId we just found
        Score score = new Score(studentId, courseId, academicYear, semester, catScore, examScore);
        score.setCourseCode(courseCode);
        score.setCourseTitle(courseTitle);
        score.setEnteredBy(enteredBy);

        scoreDAO.upsert(score);
        FileStorageManager.saveScore(score);

        return score;
    }

    /**
     * Returns all scores for a student in a given academic period.
     *
     * @param studentId    student primary key
     * @param academicYear academic year
     * @param semester     semester number
     * @return list of scores
     * @throws SQLException on database error
     */
    public List<Score> getScores(int studentId, String academicYear, int semester)
            throws SQLException {
        SecurityContext.requireAuthenticated();
        return scoreDAO.findByStudent(studentId, academicYear, semester);
    }

    // ---------------------------------------------------------------
    // Result slip
    // ---------------------------------------------------------------

    /**
     * Generates a result slip for a student, saving it to the file system.
     *
     * @param regNumber the student's registration number
     * @return the student object (with scores loaded)
     * @throws SQLException             on database error
     * @throws IllegalArgumentException if student not found
     */
    public Student generateResultSlip(String regNumber) throws SQLException {
        SecurityContext.requireAnyRole(
            SessionManager.Role.ADMIN,
            SessionManager.Role.REGISTRAR,
            SessionManager.Role.LECTURER,
            SessionManager.Role.APPROVER
        );
        Student student = studentDAO.findByRegNumber(regNumber);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + regNumber);
        }
        List<Score> scores = scoreDAO.getResultSlip(regNumber);
        FileStorageManager.generateResultSlip(student, scores);
        return student;
    }

    /**
     * Returns the full result slip data for a student.
     *
     * @param regNumber student registration number
     * @return list of scores with course details
     * @throws SQLException on database error
     */
    public List<Score> getResultSlipData(String regNumber) throws SQLException {
        SecurityContext.requireAuthenticated();
        return scoreDAO.getResultSlip(regNumber);
    }
}
