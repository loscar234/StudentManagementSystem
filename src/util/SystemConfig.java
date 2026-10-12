package util;

/**
 * SystemConfig – Centralized application-wide configuration constants.
 * Consolidates business logic values such as grading thresholds and library policies.
 */
public final class SystemConfig {

    private SystemConfig() {}

    // ---------------------------------------------------------------
    // Library Policy Constants
    // ---------------------------------------------------------------
    /** Daily fine charged per day for overdue book returns (in currency units). */
    public static final double FINE_RATE_PER_DAY = 5.00;

    /** Standard borrowing duration in days before a book becomes overdue. */
    public static final int STANDARD_BORROW_DAYS = 14;

    /** Reservation expiry window in days. */
    public static final int RESERVATION_EXPIRY_DAYS = 7;

    // ---------------------------------------------------------------
    // Academic & Grading Scale Constants
    // ---------------------------------------------------------------
    public static final double GRADE_A_MIN = 70.0;
    public static final double GRADE_B_MIN = 60.0;
    public static final double GRADE_C_MIN = 50.0;
    public static final double GRADE_D_MIN = 40.0;

    /** Maximum continuous assessment test marks. */
    public static final double MAX_CAT_SCORE = 30.0;

    /** Maximum final exam marks. */
    public static final double MAX_EXAM_SCORE = 70.0;

    // ---------------------------------------------------------------
    // Configurable Governance Policies (Default values with runtime mutability)
    // ---------------------------------------------------------------
    private static volatile int maxLecturerCreditHours = 18;
    private static volatile int maxStudentCoursesPerSemester = 5;
    private static volatile boolean enforcePrerequisites = true;

    public static int getMaxLecturerCreditHours() {
        return maxLecturerCreditHours;
    }

    public static void setMaxLecturerCreditHours(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Max lecturer credit hours must be positive.");
        }
        maxLecturerCreditHours = hours;
    }

    public static int getMaxStudentCoursesPerSemester() {
        return maxStudentCoursesPerSemester;
    }

    public static void setMaxStudentCoursesPerSemester(int maxCourses) {
        if (maxCourses <= 0) {
            throw new IllegalArgumentException("Max courses per semester must be positive.");
        }
        maxStudentCoursesPerSemester = maxCourses;
    }

    public static boolean isEnforcePrerequisites() {
        return enforcePrerequisites;
    }

    public static void setEnforcePrerequisites(boolean enforce) {
        enforcePrerequisites = enforce;
    }
}
