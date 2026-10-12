package model;

import java.time.LocalDate;
import java.sql.Timestamp;

/**
 * AcademicTerm - Represents an academic calendar term/semester lifecycle.
 */
public class AcademicTerm {
    private int id;
    private String academicYear;
    private int semester;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationStart;
    private LocalDate registrationDeadline;
    private String status; // UPCOMING, ACTIVE, CLOSED, ARCHIVED
    private boolean isCurrent;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AcademicTerm() {}

    public AcademicTerm(int id, String academicYear, int semester, LocalDate startDate, LocalDate endDate,
                        LocalDate registrationStart, LocalDate registrationDeadline, String status, boolean isCurrent) {
        this.id = id;
        this.academicYear = academicYear;
        this.semester = semester;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationStart = registrationStart;
        this.registrationDeadline = registrationDeadline;
        this.status = status;
        this.isCurrent = isCurrent;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(LocalDate registrationStart) { this.registrationStart = registrationStart; }

    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDate registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean current) { isCurrent = current; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Checks if standard registration is currently open according to calendar dates and status.
     */
    public boolean isRegistrationOpen() {
        if (!"ACTIVE".equalsIgnoreCase(status)) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(registrationStart) && !today.isAfter(registrationDeadline);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isClosed() {
        return "CLOSED".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return academicYear + " Sem " + semester + " (" + status + (isCurrent ? ", Current" : "") + ")";
    }
}
