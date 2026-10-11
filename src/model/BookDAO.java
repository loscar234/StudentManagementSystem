package model;

import db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BookDAO – Data Access Object for {@link Book} and {@link BorrowingRecord} entities.
 *
 * <p>Handles all library-related database operations including borrowing,
 * returning, reserving books, and overdue tracking.</p>
 */
public class BookDAO {

    // ---------------------------------------------------------------
    // BOOK CRUD
    // ---------------------------------------------------------------

    public void insertBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, author, edition, version, year_published, "
                   + "publisher, category, total_copies, available_copies, shelf_location) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());       ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());     ps.setString(4, book.getEdition());
            ps.setString(5, book.getVersion());    ps.setInt(6, book.getYearPublished());
            ps.setString(7, book.getPublisher());  ps.setString(8, book.getCategory());
            ps.setInt(9, book.getTotalCopies());   ps.setInt(10, book.getAvailableCopies());
            ps.setString(11, book.getShelfLocation());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) book.setId(keys.getInt(1));
        }
    }

    public Book findBookById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapBook(rs);
        }
        return null;
    }

    public List<Book> findAllBooks() throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBook(rs));
        }
        return list;
    }

    /**
     * Real-time title search – returns books whose title contains the keyword.
     *
     * @param keyword partial title string
     * @return matching books
     * @throws SQLException on database error
     */
    public List<Book> searchByTitle(String keyword) throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? ORDER BY title";
        String kw = "%" + keyword + "%";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBook(rs));
        }
        return list;
    }

    public void updateBook(Book book) throws SQLException {
        String sql = "UPDATE books SET isbn=?, title=?, author=?, edition=?, version=?, year_published=?, "
                   + "publisher=?, category=?, total_copies=?, available_copies=?, shelf_location=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());       ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());     ps.setString(4, book.getEdition());
            ps.setString(5, book.getVersion());    ps.setInt(6, book.getYearPublished());
            ps.setString(7, book.getPublisher());  ps.setString(8, book.getCategory());
            ps.setInt(9, book.getTotalCopies());   ps.setInt(10, book.getAvailableCopies());
            ps.setString(11, book.getShelfLocation()); ps.setInt(12, book.getId());
            ps.executeUpdate();
        }
    }

    public void deleteBook(int bookId) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // BORROW / RETURN (via stored procedures)
    // ---------------------------------------------------------------

    /**
     * Records a book borrowing transaction.
     *
     * @param studentId student primary key
     * @param bookId    book primary key
     * @throws SQLException on database error or no available copies
     */
    public void borrowBook(int studentId, int bookId) throws SQLException {
        String sql = "CALL sp_borrow_book(?, ?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, studentId);
            cs.setInt(2, bookId);
            cs.execute();
        }
    }

    /**
     * Records a book return transaction.
     *
     * @param recordId borrowing record primary key
     * @throws SQLException on database error
     */
    public void returnBook(int recordId) throws SQLException {
        String sql = "CALL sp_return_book(?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, recordId);
            cs.execute();
        }
    }

    // ---------------------------------------------------------------
    // RESERVE
    // ---------------------------------------------------------------

    /**
     * Creates a reservation for a book that is currently unavailable.
     *
     * @param studentId student primary key
     * @param bookId    book primary key
     * @throws SQLException on database error
     */
    public void reserveBook(int studentId, int bookId) throws SQLException {
        String sql = "INSERT INTO reservations (student_id, book_id, reservation_date, expiry_date, status) "
                   + "VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Active')";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // BORROWING RECORDS
    // ---------------------------------------------------------------

    public List<BorrowingRecord> getBorrowingHistory(int studentId) throws SQLException {
        List<BorrowingRecord> list = new ArrayList<>();
        String sql = "SELECT br.*, b.title AS book_title, b.isbn, "
                   + "CONCAT(p.first_name,' ',p.last_name) AS student_name, s.reg_number "
                   + "FROM borrowing_records br "
                   + "JOIN books b    ON b.id = br.book_id "
                   + "JOIN students s ON s.id = br.student_id "
                   + "JOIN persons p  ON p.id = s.person_id "
                   + "WHERE br.student_id = ? ORDER BY br.borrow_date DESC";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBorrowRecord(rs));
        }
        return list;
    }

    public List<BorrowingRecord> getOverdueBooks() throws SQLException {
        List<BorrowingRecord> list = new ArrayList<>();
        String sql = "SELECT br.*, b.title AS book_title, b.isbn, "
                   + "CONCAT(p.first_name,' ',p.last_name) AS student_name, s.reg_number "
                   + "FROM borrowing_records br "
                   + "JOIN books b    ON b.id = br.book_id "
                   + "JOIN students s ON s.id = br.student_id "
                   + "JOIN persons p  ON p.id = s.person_id "
                   + "WHERE br.due_date < CURDATE() AND br.return_date IS NULL "
                   + "ORDER BY br.due_date";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBorrowRecord(rs));
        }
        return list;
    }

    public List<BorrowingRecord> getCurrentBorrowings() throws SQLException {
        List<BorrowingRecord> list = new ArrayList<>();
        String sql = "SELECT br.*, b.title AS book_title, b.isbn, "
                   + "CONCAT(p.first_name,' ',p.last_name) AS student_name, s.reg_number "
                   + "FROM borrowing_records br "
                   + "JOIN books b    ON b.id = br.book_id "
                   + "JOIN students s ON s.id = br.student_id "
                   + "JOIN persons p  ON p.id = s.person_id "
                   + "WHERE br.return_date IS NULL ORDER BY br.due_date";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBorrowRecord(rs));
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Book mapBook(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("id"), rs.getString("isbn"), rs.getString("title"),
            rs.getString("author"), rs.getString("edition"), rs.getString("version"),
            rs.getInt("year_published"), rs.getString("publisher"), rs.getString("category"),
            rs.getInt("total_copies"), rs.getInt("available_copies"), rs.getString("shelf_location")
        );
    }

    private BorrowingRecord mapBorrowRecord(ResultSet rs) throws SQLException {
        BorrowingRecord br = new BorrowingRecord();
        br.setId(rs.getInt("id"));
        br.setStudentId(rs.getInt("student_id"));
        br.setStudentName(rs.getString("student_name"));
        br.setRegNumber(rs.getString("reg_number"));
        br.setBookId(rs.getInt("book_id"));
        br.setBookTitle(rs.getString("book_title"));
        br.setIsbn(rs.getString("isbn"));
        Date bd = rs.getDate("borrow_date");
        if (bd != null) br.setBorrowDate(bd.toLocalDate());
        Date dd = rs.getDate("due_date");
        if (dd != null) br.setDueDate(dd.toLocalDate());
        Date rd = rs.getDate("return_date");
        if (rd != null) br.setReturnDate(rd.toLocalDate());
        br.setStatus(rs.getString("status"));
        br.setFineAmount(rs.getDouble("fine_amount"));
        br.setNotes(rs.getString("notes"));
        return br;
    }
}
