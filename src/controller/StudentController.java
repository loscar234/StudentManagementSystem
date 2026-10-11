package controller;

import model.*;
import util.FileStorageManager;
import util.InputValidator;
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

    private final StudentDAO studentDAO;
    private final ScoreDAO   scoreDAO;
    private final CourseDAO  courseDAO; // 1. Add this field

    public StudentController() {
        this.studentDAO = new StudentDAO();
        this.scoreDAO   = new ScoreDAO();
        this.courseDAO  = new CourseDAO(); // 2. Initialize it
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
        Student s = studentDAO.findById(studentId);
        if (s == null) throw new IllegalArgumentException("Student not found: ID " + studentId);
        studentDAO.delete(studentId);
        FileStorageManager.log("Student deleted: " + s.getRegNumber());
    }

    // ---------------------------------------------------------------
    // Student queries
    // ---------------------------------------------------------------

    public Student getStudentById(int id) throws SQLException {
        return studentDAO.findById(id);
    }

    public Student getStudentByRegNumber(String regNumber) throws SQLException {
        return studentDAO.findByRegNumber(regNumber);
    }

    public List<Student> getAllStudents() throws SQLException {
        return studentDAO.findAll();
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        InputValidator.validateRequired(keyword, "Search keyword");
        return studentDAO.search(keyword.trim());
    }

    // ---------------------------------------------------------------
    // Course registration
    // ---------------------------------------------------------------

    /**
     * Registers a student for a course, enforcing the 5-course limit.
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param academicYear academic year string
     * @param semester     semester number
     * @throws SQLException             on database error or business rule violation
     * @throws IllegalArgumentException if student or course not found
     */
    public void registerCourse(int studentId, int courseId, String academicYear, int semester)
            throws SQLException {
        InputValidator.validateRequired(academicYear, "Academic year");
        InputValidator.validateSemester(semester);

        // Verify student exists
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);

        studentDAO.registerCourse(studentId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " registered for student " + student.getRegNumber());
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
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new IllegalArgumentException("Student not found: ID " + studentId);
        studentDAO.deregisterCourse(studentId, courseId, academicYear, semester);
        FileStorageManager.log("Course " + courseId + " deregistered for student " + student.getRegNumber()
                + " (" + academicYear + " Sem " + semester + ")");
    }

    /**
     * Finds a course by its course code.
     *
     * @param courseCode course code (e.g. CS101)
     * @return the Course entity, or null if not found
     * @throws SQLException on database error
     */
    public Course getCourseByCode(String courseCode) throws SQLException {
        if (courseCode == null || courseCode.trim().isEmpty()) return null;
        return courseDAO.findByCode(courseCode.trim().toUpperCase());
    }

    // ---------------------------------------------------------------
    // Score management
    // ---------------------------------------------------------------

    /**
     * Enters or updates a student's score for a course.
     *
     * @param studentId    student primary key
     * @param courseId     course primary key
     * @param courseCode   course code (for display)
     * @param courseTitle  course title (for display)
     * @param academicYear academic year
     * @param semester     semester number
     * @param catScore     CAT score (0–30)
     * @param examScore    exam score (0–70)
     * @param enteredBy    lecturer ID entering the score
     * @return the saved {@link Score}
     * @throws IllegalArgumentException if scores are out of range
     * @throws SQLException             on database error
     */


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

        if (enteredBy <= 0) {
            Integer currentUserId = SessionManager.getInstance().getCurrentUserId();
            enteredBy = (currentUserId != null) ? currentUserId : 1;
        }

        // If the ID is 0, use the courseCode to find the real ID in the database
        if (courseId <= 0 && courseCode != null) {
            Course found = courseDAO.findByCode(courseCode.trim());
            if (found != null) {
                courseId = found.getId();
            } else {
                throw new IllegalArgumentException("Could not find course ID for: " + courseCode);
            }
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
        return scoreDAO.getResultSlip(regNumber);
    }
}
