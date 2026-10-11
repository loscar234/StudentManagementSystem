package model;

import java.util.Objects;

/**
 * Book – Represents a library book in the Student Management System.
 *
 * <p>Tracks total copies, available copies, and borrowed copies.
 * Supports borrow and return operations with copy-count management.</p>
 */
public class Book {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------
    private int    id;
    private String isbn;
    private String title;
    private String author;
    private String edition;
    private String version;
    private int    yearPublished;
    private String publisher;
    private String category;
    private int    totalCopies;
    private int    availableCopies;
    private String shelfLocation;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    public Book() {}

    public Book(int id, String isbn, String title, String author, String edition,
                String version, int yearPublished, String publisher, String category,
                int totalCopies, int availableCopies, String shelfLocation) {
        this.id              = id;
        this.isbn            = isbn;
        this.title           = title;
        this.author          = author;
        this.edition         = edition;
        this.version         = version;
        this.yearPublished   = yearPublished;
        this.publisher       = publisher;
        this.category        = category;
        this.totalCopies     = totalCopies;
        this.availableCopies = availableCopies;
        this.shelfLocation   = shelfLocation;
    }

    // ---------------------------------------------------------------
    // Computed properties
    // ---------------------------------------------------------------

    /**
     * Returns the number of copies currently borrowed.
     *
     * @return borrowed copies count
     */
    public int getBorrowedCopies() {
        return totalCopies - availableCopies;
    }

    /**
     * Returns whether at least one copy is available for borrowing.
     *
     * @return true if available
     */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    // ---------------------------------------------------------------
    // Borrow / Return operations (in-memory)
    // ---------------------------------------------------------------

    /**
     * Decrements available copies when a book is borrowed.
     *
     * @throws IllegalStateException if no copies are available
     */
    public void borrowCopy() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No available copies of '" + title + "'.");
        }
        availableCopies--;
    }

    /**
     * Increments available copies when a book is returned.
     *
     * @throws IllegalStateException if all copies are already available
     */
    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies of '" + title + "' are already returned.");
        }
        availableCopies++;
    }

    // ---------------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------------

    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public String getIsbn()             { return isbn; }
    public void   setIsbn(String isbn)  { this.isbn = isbn; }

    public String getTitle()                { return title; }
    public void   setTitle(String title)    { this.title = title; }

    public String getAuthor()               { return author; }
    public void   setAuthor(String author)  { this.author = author; }

    public String getEdition()                  { return edition; }
    public void   setEdition(String edition)    { this.edition = edition; }

    public String getVersion()                  { return version; }
    public void   setVersion(String version)    { this.version = version; }

    public int    getYearPublished()                    { return yearPublished; }
    public void   setYearPublished(int yearPublished)   { this.yearPublished = yearPublished; }

    public String getPublisher()                    { return publisher; }
    public void   setPublisher(String publisher)    { this.publisher = publisher; }

    public String getCategory()                     { return category; }
    public void   setCategory(String category)      { this.category = category; }

    public int    getTotalCopies()                      { return totalCopies; }
    public void   setTotalCopies(int totalCopies)       { this.totalCopies = totalCopies; }

    public int    getAvailableCopies()                          { return availableCopies; }
    public void   setAvailableCopies(int availableCopies)       { this.availableCopies = availableCopies; }

    public String getShelfLocation()                        { return shelfLocation; }
    public void   setShelfLocation(String shelfLocation)    { this.shelfLocation = shelfLocation; }

    // ---------------------------------------------------------------
    // equals / hashCode – based on ISBN (natural key)
    // ---------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[Book] \"%s\" by %s | Edition: %s | Year: %d | Available: %d/%d",
                title, author, edition, yearPublished, availableCopies, totalCopies);
    }
}
