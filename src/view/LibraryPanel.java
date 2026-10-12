package view;

import controller.LibraryController;
import model.Book;
import model.BorrowingRecord;
import util.AccessDeniedException;
import util.SessionManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * LibraryPanel – Swing view for the library management module.
 *
 * <p>Features:
 * <ul>
 *   <li>Real-time book search (fires on every keystroke)</li>
 *   <li>Available / borrowed / overdue copy counts</li>
 *   <li>Borrow, return, and reserve operations</li>
 *   <li>Overdue books tracking with fine display</li>
 * </ul>
 * </p>
 */
public class LibraryPanel extends JPanel {

    private final LibraryController controller = new LibraryController();

    // ---- Book table ----
    private JTable            bookTable;
    private DefaultTableModel bookModel;

    // ---- Borrow/Return table ----
    private JTable            borrowTable;
    private DefaultTableModel borrowModel;

    // ---- Search ----
    private JTextField fldSearch;

    // ---- Stats labels ----
    private JLabel lblTotalBooks, lblAvailable, lblBorrowed, lblOverdue;

    // ---- Selected book ----
    private int selectedBookId = -1;

    // ---- Transaction fields ----
    private JTextField fldStudentId, fldRecordId;

    public LibraryPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        buildUI();
        loadBooks();
        loadBorrowings();
    }

    private void buildUI() {
        // ---- Stats bar ----
        add(buildStatsBar(), BorderLayout.NORTH);

        // ---- Main split ----
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setDividerLocation(350);
        mainSplit.setResizeWeight(0.55);
        mainSplit.setBorder(null);
        mainSplit.setTopComponent(buildBookPanel());
        mainSplit.setBottomComponent(buildTransactionPanel());
        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel buildStatsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        bar.setBackground(UITheme.SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        lblTotalBooks = statLabel("Total Books: –", UITheme.PRIMARY);
        lblAvailable  = statLabel("Available: –",   UITheme.SUCCESS);
        lblBorrowed   = statLabel("Borrowed: –",    UITheme.WARNING);
        lblOverdue    = statLabel("Overdue: –",     UITheme.DANGER);

        bar.add(lblTotalBooks);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(lblAvailable);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(lblBorrowed);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(lblOverdue);
        return bar;
    }

    private JLabel statLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setForeground(color);
        return lbl;
    }

    private JPanel buildBookPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Search bar with real-time listener
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchRow.setBackground(UITheme.SURFACE);
        searchRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        fldSearch = UITheme.styledField(30);
        fldSearch.setToolTipText("Type to search books in real-time...");

        JButton btnRefresh    = UITheme.successButton("Refresh");
        JButton btnDeleteBook = UITheme.dangerButton("Delete Book");

        SessionManager.Role currentRole = SessionManager.getInstance().getCurrentRole();
        boolean canManage = (currentRole == SessionManager.Role.ADMIN || currentRole == SessionManager.Role.LIBRARIAN);
        btnDeleteBook.setEnabled(canManage);

        btnRefresh.addActionListener(e -> { fldSearch.setText(""); loadBooks(); });
        btnDeleteBook.addActionListener(e -> deleteSelectedBook());

        // Real-time search on every keystroke
        fldSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { performSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { performSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { performSearch(); }
        });

        JLabel lbl = new JLabel("Search books:");
        lbl.setFont(UITheme.FONT_BODY);
        searchRow.add(lbl);
        searchRow.add(fldSearch);
        searchRow.add(btnRefresh);
        searchRow.add(btnDeleteBook);
        panel.add(searchRow, BorderLayout.NORTH);

        // Book table
        String[] cols = {"ID", "ISBN", "Title", "Author", "Edition", "Year", "Available", "Borrowed", "Shelf"};
        bookModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        bookTable = new JTable(bookModel);
        bookTable.setRowHeight(26);
        bookTable.setShowGrid(false);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    // Highlight rows with 0 available copies
                    Object avail = t.getModel().getValueAt(row, 6);
                    if (avail != null && Integer.parseInt(avail.toString()) == 0) {
                        setBackground(new Color(255, 235, 235));
                    } else {
                        setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        bookTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(50);
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(70);

        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = bookTable.getSelectedRow();
                if (row >= 0) selectedBookId = (int) bookModel.getValueAt(row, 0);
            }
        });

        JScrollPane scroll = new JScrollPane(bookTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Left: transaction controls
        JPanel controls = UITheme.sectionPanel("Library Transactions");
        controls.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        fldStudentId = UITheme.styledField(10);
        fldRecordId  = UITheme.styledField(10);

        addRow(controls, gbc, 0, "Student ID", fldStudentId);
        addRow(controls, gbc, 1, "Record ID (for return)", fldRecordId);

        SessionManager.Role currentRole = SessionManager.getInstance().getCurrentRole();
        boolean canManage = (currentRole == SessionManager.Role.ADMIN || currentRole == SessionManager.Role.LIBRARIAN);

        JButton btnBorrow   = UITheme.primaryButton("Borrow");
        JButton btnReturn   = UITheme.successButton("Return");
        JButton btnReserve  = UITheme.warningButton("Reserve");
        JButton btnOverdue  = UITheme.dangerButton("Show Overdue");
        JButton btnHistory  = UITheme.primaryButton("My History");

        btnBorrow.setEnabled(canManage);
        btnReturn.setEnabled(canManage);
        btnReserve.setEnabled(canManage);

        btnBorrow.setPreferredSize(new Dimension(110, 36));
        btnReturn.setPreferredSize(new Dimension(110, 36));
        btnReserve.setPreferredSize(new Dimension(110, 36));
        btnOverdue.setPreferredSize(new Dimension(120, 36));
        btnHistory.setPreferredSize(new Dimension(110, 36));

        JPanel btnGrid = new JPanel(new GridLayout(2, 3, 6, 6));
        btnGrid.setBackground(UITheme.SURFACE);
        btnGrid.add(btnBorrow); btnGrid.add(btnReturn); btnGrid.add(btnReserve);
        btnGrid.add(btnOverdue); btnGrid.add(btnHistory);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        controls.add(btnGrid, gbc);

        btnBorrow.addActionListener(e -> borrowBook());
        btnReturn.addActionListener(e -> returnBook());
        btnReserve.addActionListener(e -> reserveBook());
        btnOverdue.addActionListener(e -> showOverdue());
        btnHistory.addActionListener(e -> showHistory());

        controls.setPreferredSize(new Dimension(280, 0));
        panel.add(controls, BorderLayout.WEST);

        // Right: borrow/return records table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(UITheme.BACKGROUND);

        JLabel lbl = new JLabel("Current Borrowings & History");
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setForeground(new Color(123, 31, 162));
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        tablePanel.add(lbl, BorderLayout.NORTH);

        String[] cols = {"ID", "Student", "Reg No.", "Book Title", "Borrow Date", "Due Date", "Return Date", "Status", "Fine"};
        borrowModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        borrowTable = new JTable(borrowModel);
        borrowTable.setRowHeight(26);
        borrowTable.setShowGrid(false);
        borrowTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    Object status = t.getModel().getValueAt(row, 7);
                    if ("Overdue".equals(status)) setBackground(new Color(255, 220, 220));
                    else setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(borrowTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        tablePanel.add(scroll, BorderLayout.CENTER);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    // ---- Data operations ----

    private void loadBooks() {
        try {
            List<Book> books = controller.getAllBooks();
            bookModel.setRowCount(0);
            int totalAvail = 0, totalBorrowed = 0;
            for (Book b : books) {
                bookModel.addRow(new Object[]{
                    b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(),
                    b.getEdition(), b.getYearPublished(),
                    b.getAvailableCopies(), b.getBorrowedCopies(), b.getShelfLocation()
                });
                totalAvail   += b.getAvailableCopies();
                totalBorrowed += b.getBorrowedCopies();
            }
            lblTotalBooks.setText("Total Books: " + books.size());
            lblAvailable.setText("Available: "  + totalAvail);
            lblBorrowed.setText("Borrowed: "    + totalBorrowed);
        } catch (SQLException e) { showError("Error loading books: " + e.getMessage()); }
    }

    private void loadBorrowings() {
        try {
            List<BorrowingRecord> records = controller.getCurrentBorrowings();
            borrowModel.setRowCount(0);
            int overdueCount = 0;
            for (BorrowingRecord br : records) {
                if (br.isOverdue()) overdueCount++;
                borrowModel.addRow(new Object[]{
                    br.getId(), br.getStudentName(), br.getRegNumber(), br.getBookTitle(),
                    br.getBorrowDate(), br.getDueDate(),
                    br.getReturnDate() != null ? br.getReturnDate() : "–",
                    br.isOverdue() ? "Overdue" : br.getStatus(),
                    String.format("%.2f", br.computeFine())
                });
            }
            lblOverdue.setText("Overdue: " + overdueCount);
        } catch (SQLException e) { showError("Error loading borrowings: " + e.getMessage()); }
    }

    private void performSearch() {
        String kw = fldSearch.getText().trim();
        try {
            List<Book> books = controller.searchBooks(kw);
            bookModel.setRowCount(0);
            for (Book b : books) {
                bookModel.addRow(new Object[]{
                    b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(),
                    b.getEdition(), b.getYearPublished(),
                    b.getAvailableCopies(), b.getBorrowedCopies(), b.getShelfLocation()
                });
            }
        } catch (SQLException e) { showError("Search error: " + e.getMessage()); }
    }

    private void borrowBook() {
        if (selectedBookId < 0) { showError("Please select a book from the table."); return; }
        String sidStr = fldStudentId.getText().trim();
        if (sidStr.isEmpty()) { showError("Please enter a Student ID."); return; }
        try {
            int sid = Integer.parseInt(sidStr);
            controller.borrowBook(sid, selectedBookId);
            showInfo("Book borrowed successfully! Due in 14 days.");
            loadBooks(); loadBorrowings();
        } catch (AccessDeniedException ex) {
            showError("Access Denied: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            // No copies available – offer reservation
            int choice = JOptionPane.showConfirmDialog(this,
                ex.getMessage() + "\n\nWould you like to reserve it instead?",
                "No Copies Available", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) reserveBook();
        } catch (NumberFormatException ex) {
            showError("Student ID must be a number.");
        } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
    }

    private void returnBook() {
        String ridStr = fldRecordId.getText().trim();
        if (ridStr.isEmpty()) { showError("Please enter the Record ID to return."); return; }
        try {
            int rid = Integer.parseInt(ridStr);
            controller.returnBook(rid);
            showInfo("Book returned successfully!");
            fldRecordId.setText("");
            loadBooks(); loadBorrowings();
        } catch (AccessDeniedException ex) {
            showError("Access Denied: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            showError("Record ID must be a number.");
        } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
    }

    private void reserveBook() {
        if (selectedBookId < 0) { showError("Please select a book to reserve."); return; }
        String sidStr = fldStudentId.getText().trim();
        if (sidStr.isEmpty()) { showError("Please enter a Student ID."); return; }
        try {
            int sid = Integer.parseInt(sidStr);
            controller.reserveBook(sid, selectedBookId);
            showInfo("Book reserved! You will be notified when it becomes available (expires in 7 days).");
        } catch (AccessDeniedException ex) {
            showError("Access Denied: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            showError("Student ID must be a number.");
        } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
    }

    private void deleteSelectedBook() {
        if (selectedBookId < 0) {
            showError("Please select a book from the table to delete.");
            return;
        }

        int row = bookTable.getSelectedRow();
        String title = (row >= 0 && bookModel.getValueAt(row, 2) != null)
                ? bookModel.getValueAt(row, 2).toString() : "this book";

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete \"" + title + "\" (ID: " + selectedBookId + ")?",
            "Confirm Delete Book", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            controller.deleteBook(selectedBookId);
            showInfo("Book deleted successfully!");
            selectedBookId = -1;
            loadBooks();
        } catch (AccessDeniedException ex) {
            showError("Access Denied: " + ex.getMessage());
        } catch (SQLException ex) {
            showError("Database Error deleting book: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Error deleting book: " + ex.getMessage());
        }
    }

    private void showOverdue() {
        try {
            List<BorrowingRecord> overdue = controller.getOverdueBooks();
            borrowModel.setRowCount(0);
            for (BorrowingRecord br : overdue) {
                borrowModel.addRow(new Object[]{
                    br.getId(), br.getStudentName(), br.getRegNumber(), br.getBookTitle(),
                    br.getBorrowDate(), br.getDueDate(), "–", "Overdue",
                    String.format("%.2f", br.computeFine())
                });
            }
            if (overdue.isEmpty()) showInfo("No overdue books at this time.");
        } catch (SQLException e) { showError("Error: " + e.getMessage()); }
    }

    private void showHistory() {
        String sidStr = fldStudentId.getText().trim();
        if (sidStr.isEmpty()) { showError("Please enter a Student ID."); return; }
        try {
            int sid = Integer.parseInt(sidStr);
            List<BorrowingRecord> history = controller.getBorrowingHistory(sid);
            borrowModel.setRowCount(0);
            for (BorrowingRecord br : history) {
                borrowModel.addRow(new Object[]{
                    br.getId(), br.getStudentName(), br.getRegNumber(), br.getBookTitle(),
                    br.getBorrowDate(), br.getDueDate(),
                    br.getReturnDate() != null ? br.getReturnDate() : "–",
                    br.getStatus(), String.format("%.2f", br.getFineAmount())
                });
            }
            if (history.isEmpty()) showInfo("No borrowing history for this student.");
        } catch (NumberFormatException ex) {
            showError("Student ID must be a number.");
        } catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label + ":"); lbl.setFont(UITheme.FONT_BODY); p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.6; p.add(field, gbc);
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
}
