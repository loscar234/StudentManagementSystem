package util;

import model.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FileStorageManager – Simulates a file-based data storage system.
 *
 * <p>This class provides an alternative persistence mechanism that saves
 * system data to the local file system in CSV and plain-text formats.
 * It operates independently of the MySQL database, demonstrating the
 * dual-storage architecture required by the system specification.</p>
 *
 * <p>Files are stored in the {@code data/} directory relative to the
 * application root.</p>
 */
public class FileStorageManager {

    private static final String DATA_DIR        = "data/";
    private static final String STUDENTS_FILE   = DATA_DIR + "students.csv";
    private static final String LECTURERS_FILE  = DATA_DIR + "lecturers.csv";
    private static final String COURSES_FILE    = DATA_DIR + "courses.csv";
    private static final String SCORES_FILE     = DATA_DIR + "scores.csv";
    private static final String BOOKS_FILE      = DATA_DIR + "books.csv";
    private static final String BORROWS_FILE    = DATA_DIR + "borrowing_records.csv";
    private static final String LOG_FILE        = DATA_DIR + "system_log.txt";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------------------------------------------------------
    // Initialisation
    // ---------------------------------------------------------------

    /**
     * Ensures the data directory and all CSV files exist with headers.
     */
    public static void initialise() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            ensureHeader(STUDENTS_FILE,
                "StudentID,RegNumber,FirstName,LastName,Email,Phone,Gender,Programme,YearOfStudy,Semester,EnrollmentDate,Status");
            ensureHeader(LECTURERS_FILE,
                "LecturerID,StaffID,FirstName,LastName,Email,Phone,Gender,Department,Qualification,Specialization,HireDate,Status");
            ensureHeader(COURSES_FILE,
                "CourseID,CourseCode,Title,Description,CreditHours,Department,Level,Semester");
            ensureHeader(SCORES_FILE,
                "ScoreID,StudentID,CourseID,CourseCode,CourseTitle,AcademicYear,Semester,CATScore,ExamScore,TotalScore,Grade,Remarks");
            ensureHeader(BOOKS_FILE,
                "BookID,ISBN,Title,Author,Edition,Version,YearPublished,Publisher,Category,TotalCopies,AvailableCopies,ShelfLocation");
            ensureHeader(BORROWS_FILE,
                "RecordID,StudentID,RegNumber,BookID,BookTitle,BorrowDate,DueDate,ReturnDate,Status,FineAmount");
            log("FileStorageManager initialised.");
        } catch (IOException e) {
            System.err.println("[FileStorage] Initialisation error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Student file operations
    // ---------------------------------------------------------------

    /** Appends a student record to the students CSV file safely with CSV escaping. */
    public static void saveStudent(Student s) {
        String line = String.join(",",
            String.valueOf(s.getId()),
            escapeCSV(s.getRegNumber()),
            escapeCSV(s.getFirstName()),
            escapeCSV(s.getLastName()),
            escapeCSV(s.getEmail()),
            escapeCSV(s.getPhone()),
            escapeCSV(s.getGender()),
            escapeCSV(s.getProgramme()),
            String.valueOf(s.getYearOfStudy()),
            String.valueOf(s.getSemester()),
            escapeCSV(String.valueOf(s.getEnrollmentDate())),
            escapeCSV(s.getStatus())
        );
        appendLine(STUDENTS_FILE, line);
        log("Student saved: " + s.getRegNumber());
    }

    /** Rewrites the entire students CSV file from a list. */
    public static void saveAllStudents(List<Student> students) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(STUDENTS_FILE))) {
            pw.println("StudentID,RegNumber,FirstName,LastName,Email,Phone,Gender,Programme,YearOfStudy,Semester,EnrollmentDate,Status");
            for (Student s : students) {
                pw.println(String.join(",",
                    String.valueOf(s.getId()), s.getRegNumber(), s.getFirstName(), s.getLastName(),
                    s.getEmail(), s.getPhone(), s.getGender(), "\"" + s.getProgramme() + "\"",
                    String.valueOf(s.getYearOfStudy()), String.valueOf(s.getSemester()),
                    String.valueOf(s.getEnrollmentDate()), s.getStatus()
                ));
            }
            log("All students saved to file (" + students.size() + " records).");
        } catch (IOException e) {
            System.err.println("[FileStorage] Error saving students: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Score / Result slip file operations
    // ---------------------------------------------------------------

    /** Saves a score record to the scores CSV file. */
    public static void saveScore(Score score) {
        String line = String.join(",",
            String.valueOf(score.getId()),
            String.valueOf(score.getStudentId()),
            String.valueOf(score.getCourseId()),
            score.getCourseCode() != null ? score.getCourseCode() : "",
            score.getCourseTitle() != null ? "\"" + score.getCourseTitle() + "\"" : "",
            score.getAcademicYear(),
            String.valueOf(score.getSemester()),
            String.format("%.2f", score.getCatScore()),
            String.format("%.2f", score.getExamScore()),
            String.format("%.2f", score.getTotalScore()),
            score.getGrade(),
            score.getRemarks() != null ? score.getRemarks() : ""
        );
        appendLine(SCORES_FILE, line);
        log("Score saved: Student " + score.getStudentId() + " | Course " + score.getCourseCode());
    }

    /**
     * Generates and saves a formatted result slip text file for a student.
     *
     * @param student the student
     * @param scores  list of scores for the student
     */
    public static void generateResultSlip(Student student, List<Score> scores) {
        String filename = DATA_DIR + "result_slip_" + student.getRegNumber().replace("/", "_") + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("=".repeat(70));
            pw.println("                  UNIVERSITY STUDENT RESULT SLIP");
            pw.println("=".repeat(70));
            pw.printf("  Name           : %s%n", student.getFullName());
            pw.printf("  Reg. Number    : %s%n", student.getRegNumber());
            pw.printf("  Programme      : %s%n", student.getProgramme());
            pw.printf("  Year / Semester: Year %d, Semester %d%n",
                      student.getYearOfStudy(), student.getSemester());
            pw.printf("  Generated On   : %s%n", LocalDateTime.now().format(DT_FMT));
            pw.println("-".repeat(70));
            pw.printf("  %-10s %-30s %6s %6s %6s %5s%n",
                      "Code", "Course Title", "CAT", "Exam", "Total", "Grade");
            pw.println("-".repeat(70));

            double totalSum = 0;
            for (Score sc : scores) {
                pw.printf("  %-10s %-30s %6.1f %6.1f %6.1f %5s%n",
                    sc.getCourseCode(),
                    truncate(sc.getCourseTitle(), 30),
                    sc.getCatScore(),
                    sc.getExamScore(),
                    sc.getTotalScore(),
                    sc.getGrade()
                );
                totalSum += sc.getTotalScore();
            }

            pw.println("-".repeat(70));
            double avg = scores.isEmpty() ? 0 : totalSum / scores.size();
            String avgGrade = computeAverageGrade(avg);
            pw.printf("  %-42s %6.2f %5s%n", "AVERAGE", avg, avgGrade);
            pw.println("=".repeat(70));
            pw.println("  * A: 70-100 (Distinction)  B: 60-69 (Credit)  C: 50-59 (Pass)");
            pw.println("  * D: 40-49 (Marginal Pass)  F: 0-39 (Fail)");
            pw.println("=".repeat(70));

            log("Result slip generated: " + filename);
            System.out.println("[FileStorage] Result slip saved: " + filename);
        } catch (IOException e) {
            System.err.println("[FileStorage] Error generating result slip: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Book file operations
    // ---------------------------------------------------------------

    public static void saveBook(Book book) {
        String line = String.join(",",
            String.valueOf(book.getId()), book.getIsbn(),
            "\"" + book.getTitle() + "\"", "\"" + book.getAuthor() + "\"",
            book.getEdition(), book.getVersion(),
            String.valueOf(book.getYearPublished()), book.getPublisher(),
            book.getCategory(),
            String.valueOf(book.getTotalCopies()),
            String.valueOf(book.getAvailableCopies()),
            book.getShelfLocation()
        );
        appendLine(BOOKS_FILE, line);
        log("Book saved: " + book.getIsbn() + " – " + book.getTitle());
    }

    public static void saveBorrowingRecord(BorrowingRecord br) {
        String line = String.join(",",
            String.valueOf(br.getId()),
            String.valueOf(br.getStudentId()),
            br.getRegNumber() != null ? br.getRegNumber() : "",
            String.valueOf(br.getBookId()),
            "\"" + (br.getBookTitle() != null ? br.getBookTitle() : "") + "\"",
            String.valueOf(br.getBorrowDate()),
            String.valueOf(br.getDueDate()),
            br.getReturnDate() != null ? String.valueOf(br.getReturnDate()) : "N/A",
            br.getStatus(),
            String.format("%.2f", br.getFineAmount())
        );
        appendLine(BORROWS_FILE, line);
        log("Borrow record saved: Student " + br.getStudentId() + " | Book " + br.getBookId());
    }

    // ---------------------------------------------------------------
    // Logging
    // ---------------------------------------------------------------

    /**
     * Appends a timestamped entry to the system log file.
     *
     * @param message the log message
     */
    public static void log(String message) {
        String entry = "[" + LocalDateTime.now().format(DT_FMT) + "] " + message;
        appendLine(LOG_FILE, entry);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private static void ensureHeader(String filePath, String header) throws IOException {
        File f = new File(filePath);
        if (!f.exists() || f.length() == 0) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println(header);
            }
        }
    }

    private static void appendLine(String filePath, String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("[FileStorage] Error writing to " + filePath + ": " + e.getMessage());
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }

    private static String computeAverageGrade(double avg) {
        if (avg >= SystemConfig.GRADE_A_MIN) return "A";
        if (avg >= SystemConfig.GRADE_B_MIN) return "B";
        if (avg >= SystemConfig.GRADE_C_MIN) return "C";
        if (avg >= SystemConfig.GRADE_D_MIN) return "D";
        return "F";
    }

    /**
     * Escapes a value for standard CSV writing.
     * Encloses strings with commas, quotes, or newlines in double quotes,
     * and escapes existing quotes by doubling them.
     */
    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
