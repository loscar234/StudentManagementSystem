package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecturer – Extends {@link Person} and represents a university lecturer.
 *
 * <p>A lecturer can be assigned multiple courses and can search through
 * their allocated courses by code or title.</p>
 */
public class Lecturer extends Person {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private String    staffId;
    private String    department;
    private String    qualification;
    private String    specialization;
    private LocalDate hireDate;
    private String    status;

    /** In-memory list of courses assigned to this lecturer. */
    private List<Course> assignedCourses;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public Lecturer() {
        this.assignedCourses = new ArrayList<>();
    }

    public Lecturer(int id, String firstName, String lastName, String email,
                    String phone, String gender, LocalDate dateOfBirth, String address,
                    String staffId, String department, String qualification,
                    String specialization, LocalDate hireDate, String status) {
        super(id, firstName, lastName, email, phone, gender, dateOfBirth, address);
        this.staffId        = staffId;
        this.department     = department;
        this.qualification  = qualification;
        this.specialization = specialization;
        this.hireDate       = hireDate;
        this.status         = status;
        this.assignedCourses = new ArrayList<>();
    }

    // ---------------------------------------------------------------
    // Abstract method implementation
    // ---------------------------------------------------------------

    @Override
    public String getRole() { return "Lecturer"; }

    // ---------------------------------------------------------------
    // Course assignment logic
    // ---------------------------------------------------------------

    /**
     * Assigns a course to this lecturer.
     *
     * @param course the {@link Course} to assign
     */
    public void assignCourse(Course course) {
        if (!assignedCourses.contains(course)) {
            assignedCourses.add(course);
        }
    }

    /**
     * Removes a course from this lecturer's assignment list.
     *
     * @param course the course to remove
     */
    public void removeCourse(Course course) {
        assignedCourses.remove(course);
    }

    /**
     * Searches assigned courses by code or title (case-insensitive).
     *
     * @param keyword the search keyword
     * @return list of matching courses
     */
    public List<Course> searchCourses(String keyword) {
        String kw = keyword.toLowerCase();
        List<Course> results = new ArrayList<>();
        for (Course c : assignedCourses) {
            if (c.getCourseCode().toLowerCase().contains(kw)
             || c.getTitle().toLowerCase().contains(kw)) {
                results.add(c);
            }
        }
        return results;
    }

    /**
     * Returns all courses assigned to this lecturer.
     *
     * @return defensive copy of the assigned courses list
     */
    public List<Course> getAssignedCourses() {
        return new ArrayList<>(assignedCourses);
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public String    getStaffId()                       { return staffId; }
    public void      setStaffId(String staffId)         { this.staffId = staffId; }

    public String    getDepartment()                    { return department; }
    public void      setDepartment(String department)   { this.department = department; }

    public String    getQualification()                         { return qualification; }
    public void      setQualification(String qualification)     { this.qualification = qualification; }

    public String    getSpecialization()                        { return specialization; }
    public void      setSpecialization(String specialization)   { this.specialization = specialization; }

    public LocalDate getHireDate()                    { return hireDate; }
    public void      setHireDate(LocalDate hireDate)  { this.hireDate = hireDate; }

    public String    getStatus()                  { return status; }
    public void      setStatus(String status)     { this.status = status; }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[Lecturer] %s | Staff ID: %s | Dept: %s | Specialization: %s",
                getFullName(), staffId, department, specialization);
    }
}
