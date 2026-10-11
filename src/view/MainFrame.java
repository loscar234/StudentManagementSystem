package view;

import db.DatabaseConnection;
import util.FileStorageManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * MainFrame – The primary application window for the Student Management System.
 *
 * <p>Uses a {@link JTabbedPane} to host the five main functional panels:
 * Students, Lecturers, Courses, Library, and Result Slip. This class
 * serves as the top-level View in the MVC architecture.</p>
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public MainFrame() {
        UITheme.applyTheme();
        FileStorageManager.initialise();
        initComponents();
    }

    // ---------------------------------------------------------------
    // UI Initialisation
    // ---------------------------------------------------------------

    private void initComponents() {
        setTitle("Student Management System – University of Technology");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ---- Application header ----
        JPanel header = UITheme.headerPanel(
            "Student Management System",
            "University of Technology  |  Academic Year 2024/2025"
        );
        add(header, BorderLayout.NORTH);

        // ---- Tabbed content area ----
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(UITheme.BACKGROUND);
        tabbedPane.setFont(UITheme.FONT_SUBTITLE);

        tabbedPane.addTab("  Students  ",   createIcon("student"),  new StudentPanel());
        tabbedPane.addTab("  Lecturers  ",  createIcon("lecturer"), new LecturerPanel());
        tabbedPane.addTab("  Courses  ",    createIcon("course"),   new CoursePanel());
        tabbedPane.addTab("  Library  ",    createIcon("library"),  new LibraryPanel());
        tabbedPane.addTab("  Result Slip  ",createIcon("result"),   new ResultSlipPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // ---- Status bar ----
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(UITheme.PRIMARY_DARK);
        statusBar.setPreferredSize(new Dimension(0, 28));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 15, 4, 15));

        JLabel statusLabel = new JLabel("Checking database connection...");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(new Color(200, 220, 255));
        updateConnectionStatus(statusLabel);
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(UITheme.FONT_SMALL);
        versionLabel.setForeground(new Color(150, 180, 220));
        statusBar.add(versionLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Checks database connectivity via HikariCP and updates the status bar label.
     */
    private void updateConnectionStatus(JLabel statusLabel) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    return conn != null && conn.isValid(2);
                } catch (SQLException e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (connected) {
                        statusLabel.setText("🟢 Connected to DB  |  student_management_system");
                        statusLabel.setForeground(new Color(180, 250, 180));
                    } else {
                        statusLabel.setText("🔴 DB Disconnected  |  Check Database & Pool");
                        statusLabel.setForeground(new Color(255, 180, 180));
                    }
                } catch (Exception e) {
                    statusLabel.setText("🔴 DB Disconnected");
                    statusLabel.setForeground(new Color(255, 180, 180));
                }
            }
        };
        worker.execute();
    }

    /** Creates a simple coloured square icon as a tab indicator. */
    private Icon createIcon(String type) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = switch (type) {
                    case "student"  -> UITheme.PRIMARY;
                    case "lecturer" -> UITheme.SUCCESS;
                    case "course"   -> UITheme.WARNING;
                    case "library"  -> new Color(123, 31, 162);
                    case "result"   -> UITheme.DANGER;
                    default         -> UITheme.TEXT_SECONDARY;
                };
                g2.setColor(color);
                g2.fillOval(x, y + 2, 10, 10);
            }
            @Override public int getIconWidth()  { return 14; }
            @Override public int getIconHeight() { return 14; }
        };
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
