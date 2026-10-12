package model;

import java.time.LocalDateTime;

/**
 * Score – Represents a student's academic score for a specific course.
 *
 * <p>Scoring rules:
 * <ul>
 *   <li>CAT (Continuous Assessment Test): maximum 30 marks</li>
 *   <li>Exam: maximum 70 marks</li>
 *   <li>Total: CAT + Exam (maximum 100)</li>
 * </ul>
 * Grade boundaries:
 * <ul>
 *   <li>A: 70 – 100</li>
 *   <li>B: 60 – 69</li>
 *   <li>C: 50 – 59</li>
 *   <li>D: 40 – 49</li>
 *   <li>F: 0 – 39</li>
 * </ul>
 * </p>
 */
public class Score {

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------
    public static final double MAX_CAT  = 30.0;
    public static final double MAX_EXAM = 70.0;
    public static final double MAX_TOTAL = MAX_CAT + MAX_EXAM;

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private int           id;
    private int           studentId;
    private int           courseId;
    private String        courseCode;   // denormalised for display
    private String        courseTitle;  // denormalised for display
    private String        academicYear;
    private int           semester;
    private double        catScore;
    private double        examScore;
    private String        remarks;
    private int           enteredBy;
    private LocalDateTime enteredAt;
    private String        status = "DRAFT"; // DRAFT, SUBMITTED, FINALIZED
    private LocalDateTime finalizedAt;
    private Integer       finalizedBy;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public Score() {}

    public Score(int studentId, int courseId, String academicYear, int semester,
                 double catScore, double examScore) {
        this.studentId    = studentId;
        this.courseId     = courseId;
        this.academicYear = academicYear;
        this.semester     = semester;
        setCatScore(catScore);
        setExamScore(examScore);
    }

    // ---------------------------------------------------------------
    // Computed properties
    // ---------------------------------------------------------------

    /**
     * Returns the total score (CAT + Exam).
     *
     * @return total score
     */
    public double getTotalScore() {
        return catScore + examScore;
    }

    /**
     * Computes and returns the letter grade based on total score.
     *
     * @return letter grade: A, B, C, D, or F
     */
    public String getGrade() {
        double total = getTotalScore();
        if (total >= util.SystemConfig.GRADE_A_MIN) return "A";
        if (total >= util.SystemConfig.GRADE_B_MIN) return "B";
        if (total >= util.SystemConfig.GRADE_C_MIN) return "C";
        if (total >= util.SystemConfig.GRADE_D_MIN) return "D";
        return "F";
    }

    /**
     * Returns a human-readable remark for the grade.
     *
     * @return remark string
     */
    public String getGradeRemark() {
        return switch (getGrade()) {
            case "A" -> "Distinction";
            case "B" -> "Credit";
            case "C" -> "Pass";
            case "D" -> "Marginal Pass";
            default  -> "Fail";
        };
    }

    /**
     * Returns the numeric grade point equivalent.
     *
     * @return grade point (1.0 – 5.0)
     */
    public double getGradePoint() {
        return switch (getGrade()) {
            case "A" -> 5.0;
            case "B" -> 4.0;
            case "C" -> 3.0;
            case "D" -> 2.0;
            default  -> 1.0;
        };
    }

    // ---------------------------------------------------------------
    // Setters with validation
    // ---------------------------------------------------------------

    /**
     * Sets the CAT score with bounds validation.
     *
     * @param catScore score between 0 and 30
     * @throws IllegalArgumentException if out of range
     */
    public void setCatScore(double catScore) {
        if (catScore < 0 || catScore > MAX_CAT) {
            throw new IllegalArgumentException(
                "CAT score must be between 0 and " + (int) MAX_CAT + ". Provided: " + catScore);
        }
        this.catScore = catScore;
    }

    /**
     * Sets the exam score with bounds validation.
     *
     * @param examScore score between 0 and 70
     * @throws IllegalArgumentException if out of range
     */
    public void setExamScore(double examScore) {
        if (examScore < 0 || examScore > MAX_EXAM) {
            throw new IllegalArgumentException(
                "Exam score must be between 0 and " + (int) MAX_EXAM + ". Provided: " + examScore);
        }
        this.examScore = examScore;
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public int           getId()                  { return id; }
    public void          setId(int id)            { this.id = id; }

    public int           getStudentId()           { return studentId; }
    public void          setStudentId(int sid)    { this.studentId = sid; }

    public int           getCourseId()            { return courseId; }
    public void          setCourseId(int cid)     { this.courseId = cid; }

    public String        getCourseCode()                    { return courseCode; }
    public void          setCourseCode(String courseCode)   { this.courseCode = courseCode; }

    public String        getCourseTitle()                   { return courseTitle; }
    public void          setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String        getAcademicYear()                      { return academicYear; }
    public void          setAcademicYear(String academicYear)   { this.academicYear = academicYear; }

    public int           getSemester()              { return semester; }
    public void          setSemester(int semester)  { this.semester = semester; }

    public double        getCatScore()              { return catScore; }
    public double        getExamScore()             { return examScore; }

    public String        getRemarks()               { return remarks; }
    public void          setRemarks(String remarks) { this.remarks = remarks; }

    public int           getEnteredBy()             { return enteredBy; }
    public void          setEnteredBy(int eid)      { this.enteredBy = eid; }

    public LocalDateTime getEnteredAt()             { return enteredAt; }
    public void          setEnteredAt(LocalDateTime t) { this.enteredAt = t; }

    public String        getStatus()                { return status; }
    public void          setStatus(String status)   { this.status = status; }

    public LocalDateTime getFinalizedAt()           { return finalizedAt; }
    public void          setFinalizedAt(LocalDateTime fa) { this.finalizedAt = fa; }

    public Integer       getFinalizedBy()           { return finalizedBy; }
    public void          setFinalizedBy(Integer fb) { this.finalizedBy = fb; }

    public boolean       isFinalized()              { return "FINALIZED".equalsIgnoreCase(status); }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[Score] Course: %s | CAT: %.1f | Exam: %.1f | Total: %.1f | Grade: %s (%s)",
                courseCode, catScore, examScore, getTotalScore(), getGrade(), getGradeRemark());
    }
}
