package model;

import java.sql.Timestamp;

/**
 * AcademicAuditLog - Represents an immutable audit entry capturing authorized exception actions,
 * preserving original values, reason, authorizing user, and timestamp.
 */
public class AcademicAuditLog {
    private int id;
    private String entityType;
    private Integer entityId;
    private Integer studentId;
    private Integer courseId;
    private String academicYear;
    private int semester;
    private String action;
    private String previousValue;
    private String newValue;
    private String reason;
    private Integer authorizedBy;
    private String authorizedUser;
    private Timestamp createdAt;

    public AcademicAuditLog() {}

    public AcademicAuditLog(String entityType, Integer entityId, Integer studentId, Integer courseId,
                            String academicYear, int semester, String action, String previousValue,
                            String newValue, String reason, Integer authorizedBy, String authorizedUser) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.action = action;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = reason;
        this.authorizedBy = authorizedBy;
        this.authorizedUser = authorizedUser;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(Integer authorizedBy) { this.authorizedBy = authorizedBy; }

    public String getAuthorizedUser() { return authorizedUser; }
    public void setAuthorizedUser(String authorizedUser) { this.authorizedUser = authorizedUser; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
