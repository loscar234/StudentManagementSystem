package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Student – Extends {@link Person} and represents a registered university student.
 *
 * <p>A student may register for exactly <strong>5 courses</strong> per semester.
 * The class maintains an in-memory list of enrolled courses and their associated
 * scores for result-slip generation.</p>
 */
public class Student extends Person {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private String      regNumber;
    private String      programme;
    private int         yearOfStudy;
    private int         semester;
    private LocalDate   enrollmentDate;
    private String      status;

    /** In-memory list of courses this student is enrolled in. */
    private List<Course> enrolledCourses;

    /** In-memory list of scores for this student. */
    private List<Score>  scores;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public Student() {
        this.enrolledCourses = new ArrayList<>();
        this.scores          = new ArrayList<>();
    }

    public Student(int id, String firstName, String lastName, String email,
                   String phone, String gender, LocalDate dateOfBirth, String address,
                   String regNumber, String programme, int yearOfStudy, int semester,
                   LocalDate enrollmentDate, String status) {
        super(id, firstName, lastName, email, phone, gender, dateOfBirth, address);
        this.regNumber      = regNumber;
        this.programme      = programme;
        this.yearOfStudy    = yearOfStudy;
        this.semester       = semester;
        this.enrollmentDate = enrollmentDate;
        this.status         = status;
        this.enrolledCourses = new ArrayList<>();
        this.scores          = new ArrayList<>();
    }

    // ---------------------------------------------------------------
    // Abstract method implementation
    // ---------------------------------------------------------------

    @Override
    public String getRole() { return "Student"; }

    // ---------------------------------------------------------------
    // Course registration logic
    // ---------------------------------------------------------------

    /**
     * Registers a course for this student.
     * Enforces the 5-course maximum per semester.
     *
     * @param course the {@link Course} to register
     * @throws IllegalStateException if the student already has 5 courses
     */
    public void registerCourse(Course course) {
        if (enrolledCourses.size() >= 5) {
            throw new IllegalStateException(
                "Student " + regNumber + " is already registered for 5 courses this semester.");
        }
        if (!enrolledCourses.contains(course)) {
            enrolledCourses.add(course);
        }
    }

    /**
     * Removes a course from the student's registration list.
     *
     * @param course the course to remove
     */
    public void deregisterCourse(Course course) {
        enrolledCourses.remove(course);
    }

    // ---------------------------------------------------------------
    // Score / result computation
    // ---------------------------------------------------------------

    /**
     * Adds or updates a score for a specific course.
     *
     * @param score the {@link Score} object to add
     */
    public void addScore(Score score) {
        scores.removeIf(s -> s.getCourseId() == score.getCourseId());
        scores.add(score);
    }

    /**
     * Computes the average total score across all scored courses.
     *
     * @return average score, or 0.0 if no scores exist
     */
    public double computeAverageScore() {
        if (scores.isEmpty()) return 0.0;
        return scores.stream()
                     .mapToDouble(Score::getTotalScore)
                     .average()
                     .orElse(0.0);
    }

    /**
     * Computes the average grade point across all scored courses.
     * Grade points: A=5, B=4, C=3, D=2, F=1.
     *
     * @return average grade point
     */
    public double computeAverageGradePoint() {
        if (scores.isEmpty()) return 0.0;
        return scores.stream()
                     .mapToDouble(s -> gradeToPoint(s.getGrade()))
                     .average()
                     .orElse(0.0);
    }

    /**
     * Converts a letter grade to a numeric grade point.
     *
     * @param grade letter grade string
     * @return numeric grade point
     */
    private double gradeToPoint(String grade) {
        return switch (grade) {
            case "A" -> 5.0;
            case "B" -> 4.0;
            case "C" -> 3.0;
            case "D" -> 2.0;
            default  -> 1.0;
        };
    }

    /**
     * Derives the average letter grade from the average grade point.
     *
     * @return average letter grade
     */
    public String computeAverageGrade() {
        double gp = computeAverageGradePoint();
        if (gp >= 4.5) return "A";
        if (gp >= 3.5) return "B";
        if (gp >= 2.5) return "C";
        if (gp >= 1.5) return "D";
        return "F";
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public String    getRegNumber()                       { return regNumber; }
    public void      setRegNumber(String regNumber)       { this.regNumber = regNumber; }

    public String    getProgramme()                       { return programme; }
    public void      setProgramme(String programme)       { this.programme = programme; }

    public int       getYearOfStudy()                     { return yearOfStudy; }
    public void      setYearOfStudy(int yearOfStudy)      { this.yearOfStudy = yearOfStudy; }

    public int       getSemester()                        { return semester; }
    public void      setSemester(int semester)            { this.semester = semester; }

    public LocalDate getEnrollmentDate()                          { return enrollmentDate; }
    public void      setEnrollmentDate(LocalDate enrollmentDate)  { this.enrollmentDate = enrollmentDate; }

    public String    getStatus()                          { return status; }
    public void      setStatus(String status)             { this.status = status; }

    public List<Course> getEnrolledCourses()              { return new ArrayList<>(enrolledCourses); }
    public List<Score>  getScores()                       { return new ArrayList<>(scores); }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[Student] %s | Reg: %s | Programme: %s | Year %d Sem %d | Status: %s",
                getFullName(), regNumber, programme, yearOfStudy, semester, status);
    }
}
