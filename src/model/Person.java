package model;

import java.time.LocalDate;

/**
 * Person – Abstract base class representing any individual in the system.
 * Implements the common attributes shared by both Students and Lecturers.
 *
 * <p>Design Pattern: Template Method (subclasses define getRole()).</p>
 */
public abstract class Person {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    protected int       id;
    protected String    firstName;
    protected String    lastName;
    protected String    email;
    protected String    phone;
    protected String    gender;
    protected LocalDate dateOfBirth;
    protected String    address;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Default constructor required for ORM-style population. */
    public Person() {}

    /**
     * Full constructor.
     *
     * @param id          database primary key
     * @param firstName   person's first name
     * @param lastName    person's last name
     * @param email       unique email address
     * @param phone       contact phone number
     * @param gender      gender string (Male / Female / Other)
     * @param dateOfBirth date of birth
     * @param address     residential address
     */
    public Person(int id, String firstName, String lastName, String email,
                  String phone, String gender, LocalDate dateOfBirth, String address) {
        this.id          = id;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.email       = email;
        this.phone       = phone;
        this.gender      = gender;
        this.dateOfBirth = dateOfBirth;
        this.address     = address;
    }

    // ---------------------------------------------------------------
    // Abstract method – subclasses must define their role
    // ---------------------------------------------------------------

    /**
     * Returns the role of this person in the system.
     *
     * @return role string, e.g. "Student" or "Lecturer"
     */
    public abstract String getRole();

    // ---------------------------------------------------------------
    // Convenience methods
    // ---------------------------------------------------------------

    /** Returns the full name as "FirstName LastName". */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public int       getId()          { return id; }
    public void      setId(int id)    { this.id = id; }

    public String    getFirstName()                 { return firstName; }
    public void      setFirstName(String firstName) { this.firstName = firstName; }

    public String    getLastName()                  { return lastName; }
    public void      setLastName(String lastName)   { this.lastName = lastName; }

    public String    getEmail()               { return email; }
    public void      setEmail(String email)   { this.email = email; }

    public String    getPhone()               { return phone; }
    public void      setPhone(String phone)   { this.phone = phone; }

    public String    getGender()                { return gender; }
    public void      setGender(String gender)   { this.gender = gender; }

    public LocalDate getDateOfBirth()                     { return dateOfBirth; }
    public void      setDateOfBirth(LocalDate dateOfBirth){ this.dateOfBirth = dateOfBirth; }

    public String    getAddress()                 { return address; }
    public void      setAddress(String address)   { this.address = address; }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[%s] %s | Email: %s | Phone: %s",
                getRole(), getFullName(), email, phone);
    }
}
