package util;

import java.util.regex.Pattern;

/**
 * InputValidator – Centralised input validation utility.
 *
 * <p>Provides static methods for validating all user-supplied data
 * before it is passed to the model layer. Throws descriptive
 * {@link IllegalArgumentException} instances on validation failure.</p>
 */
public class InputValidator {

    // ---------------------------------------------------------------
    // Regex patterns
    // ---------------------------------------------------------------
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9+\\-\\s]{7,15}$");

    private static final Pattern REG_NUMBER_PATTERN =
        Pattern.compile("^[A-Z]{2,4}/[0-9]{3,4}/[0-9]{4}$");

    private static final Pattern STAFF_ID_PATTERN =
        Pattern.compile("^STAFF/[0-9]{3,4}$");

    private static final Pattern COURSE_CODE_PATTERN =
        Pattern.compile("^[A-Z]{2,4}[0-9]{3}$");

    private static final Pattern ISBN_PATTERN =
        Pattern.compile("^(97[89]-?[0-9]{1,5}-?[0-9]{1,7}-?[0-9]{1,7}-?[0-9]|[0-9]{9}[0-9X])$");

    // ---------------------------------------------------------------
    // String / Name validation
    // ---------------------------------------------------------------

    /**
     * Validates that a name field is non-null, non-empty, and within length bounds.
     *
     * @param name      the name string
     * @param fieldName the field label for error messages
     * @throws IllegalArgumentException if invalid
     */
    public static void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException(fieldName + " must be at least 2 characters.");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 100 characters.");
        }
    }

    /**
     * Validates that a required text field is non-null and non-empty.
     *
     * @param value     the value to check
     * @param fieldName the field label for error messages
     * @throws IllegalArgumentException if empty
     */
    public static void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    // ---------------------------------------------------------------
    // Email validation
    // ---------------------------------------------------------------

    /**
     * Validates an email address format.
     *
     * @param email the email string
     * @throws IllegalArgumentException if format is invalid
     */
    public static void validateEmail(String email) {
        validateRequired(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }

    // ---------------------------------------------------------------
    // Phone validation
    // ---------------------------------------------------------------

    public static void validatePhone(String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
                throw new IllegalArgumentException("Invalid phone number format: " + phone);
            }
        }
    }

    // ---------------------------------------------------------------
    // Registration / Staff ID validation
    // ---------------------------------------------------------------

    /**
     * Validates a student registration number (e.g. CS/001/2023).
     *
     * @param regNumber the registration number
     * @throws IllegalArgumentException if format is invalid
     */
    public static void validateRegNumber(String regNumber) {
        validateRequired(regNumber, "Registration number");
        if (!REG_NUMBER_PATTERN.matcher(regNumber.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException(
                "Invalid registration number format. Expected: XX/000/0000 (e.g. CS/001/2023)");
        }
    }

    /**
     * Validates a staff ID (e.g. STAFF/001).
     *
     * @param staffId the staff ID
     * @throws IllegalArgumentException if format is invalid
     */
    public static void validateStaffId(String staffId) {
        validateRequired(staffId, "Staff ID");
        if (!STAFF_ID_PATTERN.matcher(staffId.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException(
                "Invalid staff ID format. Expected: STAFF/000 (e.g. STAFF/001)");
        }
    }

    // ---------------------------------------------------------------
    // Course code validation
    // ---------------------------------------------------------------

    public static void validateCourseCode(String courseCode) {
        validateRequired(courseCode, "Course code");
        if (!COURSE_CODE_PATTERN.matcher(courseCode.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException(
                "Invalid course code format. Expected: XX000 (e.g. CS101)");
        }
    }

    // ---------------------------------------------------------------
    // Score validation
    // ---------------------------------------------------------------

    /**
     * Validates a CAT score (0–30).
     *
     * @param score the CAT score
     * @throws IllegalArgumentException if out of range
     */
    public static void validateCatScore(double score) {
        if (score < 0 || score > 30) {
            throw new IllegalArgumentException("CAT score must be between 0 and 30. Provided: " + score);
        }
    }

    /**
     * Validates an exam score (0–70).
     *
     * @param score the exam score
     * @throws IllegalArgumentException if out of range
     */
    public static void validateExamScore(double score) {
        if (score < 0 || score > 70) {
            throw new IllegalArgumentException("Exam score must be between 0 and 70. Provided: " + score);
        }
    }

    // ---------------------------------------------------------------
    // ISBN validation
    // ---------------------------------------------------------------

    public static void validateISBN(String isbn) {
        validateRequired(isbn, "ISBN");
        String cleaned = isbn.replace("-", "").replace(" ", "");
        if (cleaned.length() != 10 && cleaned.length() != 13) {
            throw new IllegalArgumentException("ISBN must be 10 or 13 digits (with or without hyphens).");
        }
    }

    // ---------------------------------------------------------------
    // Year validation
    // ---------------------------------------------------------------

    public static void validateYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100. Provided: " + year);
        }
    }

    // ---------------------------------------------------------------
    // Year of study / Semester
    // ---------------------------------------------------------------

    public static void validateYearOfStudy(int year) {
        if (year < 1 || year > 6) {
            throw new IllegalArgumentException("Year of study must be between 1 and 6.");
        }
    }

    public static void validateSemester(int semester) {
        if (semester != 1 && semester != 2) {
            throw new IllegalArgumentException("Semester must be 1 or 2.");
        }
    }

    // ---------------------------------------------------------------
    // Numeric parsing helpers
    // ---------------------------------------------------------------

    /**
     * Parses a string as a double, throwing a user-friendly exception on failure.
     *
     * @param value     the string to parse
     * @param fieldName the field label for error messages
     * @return the parsed double
     * @throws IllegalArgumentException if not a valid number
     */
    public static double parseDouble(String value, String fieldName) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number. Provided: " + value);
        }
    }

    public static int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer. Provided: " + value);
        }
    }
}
