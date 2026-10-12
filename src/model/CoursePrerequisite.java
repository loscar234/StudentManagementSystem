package model;

/**
 * CoursePrerequisite - Represents an academic prerequisite requirement between two courses.
 */
public class CoursePrerequisite {
    private int id;
    private int courseId;
    private int prerequisiteCourseId;
    private String prerequisiteCourseCode;
    private String prerequisiteCourseTitle;
    private String minGrade;

    public CoursePrerequisite() {}

    public CoursePrerequisite(int id, int courseId, int prerequisiteCourseId, String minGrade) {
        this.id = id;
        this.courseId = courseId;
        this.prerequisiteCourseId = prerequisiteCourseId;
        this.minGrade = minGrade;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public int getPrerequisiteCourseId() { return prerequisiteCourseId; }
    public void setPrerequisiteCourseId(int prerequisiteCourseId) { this.prerequisiteCourseId = prerequisiteCourseId; }

    public String getPrerequisiteCourseCode() { return prerequisiteCourseCode; }
    public void setPrerequisiteCourseCode(String prerequisiteCourseCode) { this.prerequisiteCourseCode = prerequisiteCourseCode; }

    public String getPrerequisiteCourseTitle() { return prerequisiteCourseTitle; }
    public void setPrerequisiteCourseTitle(String prerequisiteCourseTitle) { this.prerequisiteCourseTitle = prerequisiteCourseTitle; }

    public String getMinGrade() { return minGrade; }
    public void setMinGrade(String minGrade) { this.minGrade = minGrade; }

    @Override
    public String toString() {
        return (prerequisiteCourseCode != null ? prerequisiteCourseCode : ("ID:" + prerequisiteCourseId)) 
                + " (Min Grade: " + minGrade + ")";
    }
}
