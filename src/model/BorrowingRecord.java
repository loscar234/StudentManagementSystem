package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * BorrowingRecord – Represents a library borrowing transaction.
 *
 * <p>Tracks borrow date, due date, return date, and status.
 * Automatically computes overdue days and fine amounts.</p>
 */
public class BorrowingRecord {

    /** Fine rate per overdue day (currency units), sourced from SystemConfig. */
    public static final double FINE_PER_DAY = util.SystemConfig.FINE_RATE_PER_DAY;

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private int       id;
    private int       studentId;
    private String    studentName;   // denormalised for display
    private String    regNumber;     // denormalised for display
    private int       bookId;
    private String    bookTitle;     // denormalised for display
    private String    isbn;          // denormalised for display
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String    status;        // Borrowed | Returned | Overdue | Reserved
    private double    fineAmount;
    private String    notes;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public BorrowingRecord() {}

    public BorrowingRecord(int studentId, int bookId, LocalDate borrowDate, LocalDate dueDate) {
        this.studentId  = studentId;
        this.bookId     = bookId;
        this.borrowDate = borrowDate;
        this.dueDate    = dueDate;
        this.status     = "Borrowed";
        this.fineAmount = 0.00;
    }

    // ---------------------------------------------------------------
    // Computed properties
    // ---------------------------------------------------------------

    /**
     * Returns the number of days the book is overdue.
     * Returns 0 if not yet overdue or already returned.
     *
     * @return overdue days
     */
    public long getOverdueDays() {
        if (returnDate != null) return 0;
        LocalDate today = LocalDate.now();
        if (today.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, today);
        }
        return 0;
    }

    /**
     * Computes the fine based on overdue days using centralized config.
     *
     * @return computed fine amount
     */
    public double computeFine() {
        return getOverdueDays() * util.SystemConfig.FINE_RATE_PER_DAY;
    }

    /**
     * Returns true if this record is currently overdue.
     *
     * @return true if overdue
     */
    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public int       getId()                  { return id; }
    public void      setId(int id)            { this.id = id; }

    public int       getStudentId()           { return studentId; }
    public void      setStudentId(int sid)    { this.studentId = sid; }

    public String    getStudentName()                       { return studentName; }
    public void      setStudentName(String studentName)     { this.studentName = studentName; }

    public String    getRegNumber()                     { return regNumber; }
    public void      setRegNumber(String regNumber)     { this.regNumber = regNumber; }

    public int       getBookId()              { return bookId; }
    public void      setBookId(int bid)       { this.bookId = bid; }

    public String    getBookTitle()                   { return bookTitle; }
    public void      setBookTitle(String bookTitle)   { this.bookTitle = bookTitle; }

    public String    getIsbn()              { return isbn; }
    public void      setIsbn(String isbn)   { this.isbn = isbn; }

    public LocalDate getBorrowDate()                      { return borrowDate; }
    public void      setBorrowDate(LocalDate borrowDate)  { this.borrowDate = borrowDate; }

    public LocalDate getDueDate()                   { return dueDate; }
    public void      setDueDate(LocalDate dueDate)  { this.dueDate = dueDate; }

    public LocalDate getReturnDate()                        { return returnDate; }
    public void      setReturnDate(LocalDate returnDate)    { this.returnDate = returnDate; }

    public String    getStatus()                  { return status; }
    public void      setStatus(String status)     { this.status = status; }

    public double    getFineAmount()                    { return fineAmount; }
    public void      setFineAmount(double fineAmount)   { this.fineAmount = fineAmount; }

    public String    getNotes()               { return notes; }
    public void      setNotes(String notes)   { this.notes = notes; }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[Borrow] %s | Book: %s | Due: %s | Status: %s | Fine: %.2f",
                regNumber, bookTitle, dueDate, status, fineAmount);
    }
}
