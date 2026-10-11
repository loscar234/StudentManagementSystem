package model;

import java.util.Objects;

/**
 * Course – Represents a university course unit.
 *
 * <p>Each course has a unique code, a title, credit hours, and belongs
 * to a department. Courses are allocated to both students and lecturers.</p>
 */
public class Course {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private int    id;
    private String courseCode;
    private String title;
    private String description;
    private int    creditHours;
    private String department;
    private int    level;
    private int    semester;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public Course() {}

    public Course(int id, String courseCode, String title, String description,
                  int creditHours, String department, int level, int semester) {
        this.id          = id;
        this.courseCode  = courseCode;
        this.title       = title;
        this.description = description;
        this.creditHours = creditHours;
        this.department  = department;
        this.level       = level;
        this.semester    = semester;
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public int    getId()                   { return id; }
    public void   setId(int id)             { this.id = id; }

    public String getCourseCode()                     { return courseCode; }
    public void   setCourseCode(String courseCode)    { this.courseCode = courseCode; }

    public String getTitle()                { return title; }
    public void   setTitle(String title)    { this.title = title; }

    public String getDescription()                    { return description; }
    public void   setDescription(String description)  { this.description = description; }

    public int    getCreditHours()                    { return creditHours; }
    public void   setCreditHours(int creditHours)     { this.creditHours = creditHours; }

    public String getDepartment()                     { return department; }
    public void   setDepartment(String department)    { this.department = department; }

    public int    getLevel()                { return level; }
    public void   setLevel(int level)       { this.level = level; }

    public int    getSemester()             { return semester; }
    public void   setSemester(int semester) { this.semester = semester; }

    // ---------------------------------------------------------------
    // equals / hashCode – based on courseCode (natural key)
    // ---------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return Objects.equals(courseCode, course.courseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[%s] %s (%d credits) – %s", courseCode, title, creditHours, department);
    }
}
