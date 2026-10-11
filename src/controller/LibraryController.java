package controller;

import model.*;
import util.FileStorageManager;
import util.InputValidator;

import java.sql.SQLException;
import java.util.List;

/**
 * LibraryController – Handles all business logic for the library module.
 *
 * <p>Manages book search, borrowing, returning, and reservation workflows.
 * Enforces business rules such as availability checks before borrowing
 * and automatic reservation suggestions when no copies are available.</p>
 */
public class LibraryController {

    private final BookDAO bookDAO;

    public LibraryController() {
        this.bookDAO = new BookDAO();
    }

    // ---------------------------------------------------------------
    // Book CRUD
    // ---------------------------------------------------------------

    public Book addBook(String isbn, String title, String author, String edition,
                        String version, int yearPublished, String publisher,
                        String category, int totalCopies, String shelfLocation)
            throws SQLException {

        InputValidator.validateISBN(isbn);
        InputValidator.validateRequired(title, "Book title");
        InputValidator.validateRequired(author, "Author");
        InputValidator.validateYear(yearPublished);

        if (totalCopies < 1) {
            throw new IllegalArgumentException("Total copies must be at least 1.");
        }

        Book book = new Book(0, isbn.trim(), title.trim(), author.trim(), edition, version,
                yearPublished, publisher, category, totalCopies, totalCopies, shelfLocation);
        bookDAO.insertBook(book);
        FileStorageManager.saveBook(book);
        FileStorageManager.log("New book added: " + isbn + " – " + title);
        return book;
    }

    public void updateBook(Book book) throws SQLException {
        InputValidator.validateISBN(book.getIsbn());
        InputValidator.validateRequired(book.getTitle(), "Book title");
        bookDAO.updateBook(book);
        FileStorageManager.log("Book updated: " + book.getIsbn());
    }

    public void deleteBook(int bookId) throws SQLException {
        Book b = bookDAO.findBookById(bookId);
        if (b == null) throw new IllegalArgumentException("Book not found: ID " + bookId);
        bookDAO.deleteBook(bookId);
        FileStorageManager.log("Book deleted: ID " + bookId);
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    public List<Book> getAllBooks() throws SQLException {
        return bookDAO.findAllBooks();
    }

    /**
     * Real-time title/author search – suitable for keystroke-triggered queries.
     *
     * @param keyword partial title or author string
     * @return matching books
     * @throws SQLException on database error
     */
    public List<Book> searchBooks(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookDAO.findAllBooks();
        }
        return bookDAO.searchByTitle(keyword.trim());
    }

    // ---------------------------------------------------------------
    // Borrow
    // ---------------------------------------------------------------

    /**
     * Processes a book borrowing request for a student.
     * If no copies are available, suggests reservation instead.
     *
     * @param studentId student primary key
     * @param bookId    book primary key
     * @throws SQLException             on database error
     * @throws IllegalStateException    if no copies available (caught by View to offer reservation)
     */
    public void borrowBook(int studentId, int bookId) throws SQLException {
        Book book = bookDAO.findBookById(bookId);
        if (book == null) throw new IllegalArgumentException("Book not found: ID " + bookId);

        if (!book.isAvailable()) {
            throw new IllegalStateException(
                "No available copies of \"" + book.getTitle() + "\". Would you like to reserve it?");
        }

        bookDAO.borrowBook(studentId, bookId);
        FileStorageManager.log("Book borrowed: Student " + studentId + " | Book " + bookId);
    }

    // ---------------------------------------------------------------
    // Return
    // ---------------------------------------------------------------

    /**
     * Processes a book return.
     *
     * @param recordId borrowing record primary key
     * @throws SQLException on database error
     */
    public void returnBook(int recordId) throws SQLException {
        bookDAO.returnBook(recordId);
        FileStorageManager.log("Book returned: Record ID " + recordId);
    }

    // ---------------------------------------------------------------
    // Reserve
    // ---------------------------------------------------------------

    /**
     * Reserves a book for a student when no copies are available.
     *
     * @param studentId student primary key
     * @param bookId    book primary key
     * @throws SQLException on database error
     */
    public void reserveBook(int studentId, int bookId) throws SQLException {
        Book book = bookDAO.findBookById(bookId);
        if (book == null) throw new IllegalArgumentException("Book not found: ID " + bookId);

        bookDAO.reserveBook(studentId, bookId);
        FileStorageManager.log("Book reserved: Student " + studentId + " | Book " + bookId);
    }

    // ---------------------------------------------------------------
    // Borrowing records
    // ---------------------------------------------------------------

    public List<BorrowingRecord> getBorrowingHistory(int studentId) throws SQLException {
        return bookDAO.getBorrowingHistory(studentId);
    }

    public List<BorrowingRecord> getOverdueBooks() throws SQLException {
        return bookDAO.getOverdueBooks();
    }

    public List<BorrowingRecord> getCurrentBorrowings() throws SQLException {
        return bookDAO.getCurrentBorrowings();
    }
}
