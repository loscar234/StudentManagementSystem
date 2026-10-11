package view;

import controller.StudentController;
import model.Course;
import model.Score;
import model.Student;
import util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * StudentPanel – Swing view for all student-related operations.
 *
 * <p>Provides a split-pane layout:
 * <ul>
 *   <li>Left: search bar and student table</li>
 *   <li>Right: tabbed form for Add/Edit, Courses, and Scores</li>
 * </ul>
 * </p>
 */
public class StudentPanel extends JPanel {

    private final StudentController controller = new StudentController();

    // ---- Table ----
    private JTable           studentTable;
    private DefaultTableModel tableModel;

    // ---- Form fields ----
    private JTextField fldFirstName, fldLastName, fldEmail, fldPhone;
    private JTextField fldAddress, fldRegNumber, fldProgramme;
    private JComboBox<String> cmbGender, cmbYear, cmbSemester, cmbStatus;
    private JTextField fldDOB, fldEnrollDate;

    // ---- Search ----
    private JTextField fldSearch;

    // ---- Score entry ----
    private JTextField fldCatScore, fldExamScore;
    private JComboBox<String> cmbScoreCourse;

    // ---- Academic Year / Semester Dropdowns ----
    private JComboBox<String> cmbCoursesAcadYear, cmbCoursesSemester;
    private JComboBox<String> cmbScoreAcadYear, cmbScoreSemester;

    // ---- Buttons ----
    private JButton btnSave, btnUpdate, btnDelete, btnClear, btnSearch;
    private JButton btnEnterScore, btnViewScores;

    // ---- State ----
    private int selectedStudentId = -1;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    public StudentPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        buildUI();
        loadStudents();
    }

    // ---------------------------------------------------------------
    // UI Construction
    // ---------------------------------------------------------------

    private void buildUI() {
        // ---- Search bar ----
        JPanel searchBar = buildSearchBar();
        add(searchBar, BorderLayout.NORTH);

        // ---- Split pane ----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(560);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        split.setLeftComponent(buildTablePanel());
        split.setRightComponent(buildFormPanel());

        add(split, BorderLayout.CENTER);
    }

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        JLabel lbl = new JLabel("Search:");
        lbl.setFont(UITheme.FONT_BODY);

        fldSearch = UITheme.styledField(25);
        fldSearch.setToolTipText("Search by name, reg number, or programme");

        btnSearch = UITheme.primaryButton("Search");
        JButton btnRefresh = UITheme.successButton("Refresh");

        btnSearch.addActionListener(e -> performSearch());
        btnRefresh.addActionListener(e -> { fldSearch.setText(""); loadStudents(); });
        fldSearch.addActionListener(e -> performSearch());

        panel.add(lbl);
        panel.add(fldSearch);
        panel.add(btnSearch);
        panel.add(btnRefresh);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JLabel title = new JLabel("Registered Students");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Reg Number", "Full Name", "Programme", "Year", "Sem", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(26);
        studentTable.setShowGrid(false);
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getTableHeader().setReorderingAllowed(false);

        // Alternating row colours
        studentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Column widths
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        studentTable.getColumnModel().getColumn(4).setPreferredWidth(40);
        studentTable.getColumnModel().getColumn(5).setPreferredWidth(40);
        studentTable.getColumnModel().getColumn(6).setPreferredWidth(70);

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateFormFromTable();
        });

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Details", buildDetailsTab());
        tabs.addTab("Courses",         buildCoursesTab());
        tabs.addTab("Scores",          buildScoresTab());

        // ADD THIS LISTENER:
        tabs.addChangeListener(e -> {
            // If the "Scores" tab (index 2) is selected, refresh the list
            if (tabs.getSelectedIndex() == 2) {
                refreshScoreCourseList();
            }
        });


        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    // ---- Details tab ----
    private JPanel buildDetailsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);

        JPanel form = UITheme.sectionPanel("Student Information");
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        fldFirstName  = UITheme.styledField(15);
        fldLastName   = UITheme.styledField(15);
        fldEmail      = UITheme.styledField(20);
        fldPhone      = UITheme.styledField(15);
        fldAddress    = UITheme.styledField(20);
        fldRegNumber  = UITheme.styledField(15);
        fldProgramme  = UITheme.styledField(20);
        fldDOB        = UITheme.styledField(12);
        fldEnrollDate = UITheme.styledField(12);
        cmbGender     = UITheme.styledCombo(new String[]{"Male", "Female", "Other"});
        cmbYear       = UITheme.styledCombo(new String[]{"1","2","3","4","5","6"});
        cmbSemester   = UITheme.styledCombo(new String[]{"1","2"});
        cmbStatus     = UITheme.styledCombo(new String[]{"Active","Suspended","Graduated","Deferred"});

        fldDOB.setToolTipText("Format: YYYY-MM-DD");
        fldEnrollDate.setToolTipText("Format: YYYY-MM-DD");

        addFormRow(form, gbc, 0, "First Name *",    fldFirstName);
        addFormRow(form, gbc, 1, "Last Name *",     fldLastName);
        addFormRow(form, gbc, 2, "Email *",         fldEmail);
        addFormRow(form, gbc, 3, "Phone",           fldPhone);
        addFormRow(form, gbc, 4, "Gender *",        cmbGender);
        addFormRow(form, gbc, 5, "Date of Birth",   fldDOB);
        addFormRow(form, gbc, 6, "Address",         fldAddress);
        addFormRow(form, gbc, 7, "Reg. Number *",   fldRegNumber);
        addFormRow(form, gbc, 8, "Programme *",     fldProgramme);
        addFormRow(form, gbc, 9, "Year of Study *", cmbYear);
        addFormRow(form, gbc,10, "Semester *",      cmbSemester);
        addFormRow(form, gbc,11, "Enrollment Date *",fldEnrollDate);
        addFormRow(form, gbc,12, "Status",          cmbStatus);

        panel.add(new JScrollPane(form), BorderLayout.CENTER);
        panel.add(buildActionButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        panel.setBackground(UITheme.BACKGROUND);

        btnSave   = UITheme.primaryButton("Add");
        btnUpdate = UITheme.warningButton("Update");
        btnDelete = UITheme.dangerButton("Delete");
        btnClear  = UITheme.successButton("Clear");

        btnSave.addActionListener(e -> saveStudent());
        btnUpdate.addActionListener(e -> updateStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnClear.addActionListener(e -> clearForm());

        panel.add(btnSave);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnClear);
        return panel;
    }

    // ---- Courses tab ----
    private JPanel buildCoursesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel info = new JLabel("<html><i>Select a student, then click 'View Courses' to see their enrolled courses.</i></html>");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SECONDARY);

        cmbCoursesAcadYear = UITheme.styledCombo(new String[]{"2023/2024", "2024/2025", "2025/2026", "2026/2027"});
        cmbCoursesAcadYear.setSelectedItem("2024/2025");
        cmbCoursesSemester = UITheme.styledCombo(new String[]{"1", "2"});

        JButton btnViewCourses = UITheme.primaryButton("View Courses");
        JButton btnDropCourse = UITheme.dangerButton("Drop Course");

        String[] colsCourse = {"Code", "Title", "Credits", "Department", "Level"};
        DefaultTableModel courseModel = new DefaultTableModel(colsCourse, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable courseTable = new JTable(courseModel);
        courseTable.setRowHeight(26);

        btnViewCourses.addActionListener(e -> {
            if (selectedStudentId < 0) {
                showError("Please select a student first.");
                return;
            }
            try {
                String acadYear = (String) cmbCoursesAcadYear.getSelectedItem();
                int sem = Integer.parseInt((String) cmbCoursesSemester.getSelectedItem());
                List<Course> courses = controller.getEnrolledCourses(selectedStudentId, acadYear, sem);
                courseModel.setRowCount(0);
                for (Course c : courses) {
                    courseModel.addRow(new Object[]{
                        c.getCourseCode(), c.getTitle(), c.getCreditHours(),
                        c.getDepartment(), c.getLevel()
                    });
                }
                if (courses.isEmpty()) showInfo("No courses enrolled for this student in " + acadYear + " Sem " + sem + ".");
            } catch (SQLException ex) {
                showError("Database Error loading courses: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Error loading courses: " + ex.getMessage());
            }
        });

        btnDropCourse.addActionListener(e -> {
            if (selectedStudentId < 0) {
                showError("Please select a student first.");
                return;
            }
            int selectedRow = courseTable.getSelectedRow();
            if (selectedRow < 0) {
                showError("Please select a course from the table to drop.");
                return;
            }

            String courseCode = (String) courseModel.getValueAt(selectedRow, 0);
            String courseTitle = (String) courseModel.getValueAt(selectedRow, 1);
            String acadYear = (String) cmbCoursesAcadYear.getSelectedItem();
            int sem = Integer.parseInt((String) cmbCoursesSemester.getSelectedItem());

            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to drop course " + courseCode + " (" + courseTitle + ") for " + acadYear + " Semester " + sem + "?",
                "Confirm Drop Course", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                Course course = controller.getCourseByCode(courseCode);
                if (course == null) {
                    showError("Could not find course in database: " + courseCode);
                    return;
                }
                controller.deregisterCourse(selectedStudentId, course.getId(), acadYear, sem);
                showInfo("Course " + courseCode + " dropped successfully!");

                // Refresh courses table and score dropdown
                btnViewCourses.doClick();
                refreshScoreCourseList();
            } catch (IllegalArgumentException ex) {
                showError("Validation Error: " + ex.getMessage());
            } catch (SQLException ex) {
                showError("Database Error dropping course: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Error dropping course: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.setBackground(UITheme.BACKGROUND);
        top.add(new JLabel("Year:"));
        top.add(cmbCoursesAcadYear);
        top.add(new JLabel("Sem:"));
        top.add(cmbCoursesSemester);
        top.add(btnViewCourses);
        top.add(btnDropCourse);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(courseTable), BorderLayout.CENTER);
        return panel;
    }

    // ---- Scores tab ----
    private JPanel buildScoresTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Score entry form
        JPanel entryForm = UITheme.sectionPanel("Enter Score");
        entryForm.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbScoreAcadYear = UITheme.styledCombo(new String[]{"2023/2024", "2024/2025", "2025/2026", "2026/2027"});
        cmbScoreAcadYear.setSelectedItem("2024/2025");
        cmbScoreSemester = UITheme.styledCombo(new String[]{"1", "2"});

        cmbScoreAcadYear.addActionListener(e -> refreshScoreCourseList());
        cmbScoreSemester.addActionListener(e -> refreshScoreCourseList());

        cmbScoreCourse = new JComboBox<>();
        cmbScoreCourse.setFont(UITheme.FONT_BODY);
        fldCatScore  = UITheme.styledField(8);
        fldExamScore = UITheme.styledField(8);

        addFormRow(entryForm, gbc, 0, "Academic Year", cmbScoreAcadYear);
        addFormRow(entryForm, gbc, 1, "Semester",      cmbScoreSemester);
        addFormRow(entryForm, gbc, 2, "Course",        cmbScoreCourse);
        addFormRow(entryForm, gbc, 3, "CAT (0–30)",    fldCatScore);
        addFormRow(entryForm, gbc, 4, "Exam (0–70)",   fldExamScore);

        btnEnterScore = UITheme.primaryButton("Save Score");
        btnViewScores = UITheme.successButton("View Scores");

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.setBackground(UITheme.SURFACE);
        btnRow.add(btnEnterScore);
        btnRow.add(btnViewScores);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        entryForm.add(btnRow, gbc);

        // Score results table
        String[] cols = {"Course Code", "Course Title", "CAT", "Exam", "Total", "Grade", "Remark"};
        DefaultTableModel scoreModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable scoreTable = new JTable(scoreModel);
        scoreTable.setRowHeight(26);

        btnEnterScore.addActionListener(e -> {
            if (selectedStudentId < 0) { showError("Please select a student first."); return; }
            try {
                String courseEntry = (String) cmbScoreCourse.getSelectedItem();
                if (courseEntry == null) { showError("Please select a course."); return; }
                String[] parts = courseEntry.split(" - ");
                String courseCode = parts[0].trim();

                double cat  = Double.parseDouble(fldCatScore.getText().trim());
                double exam = Double.parseDouble(fldExamScore.getText().trim());

                String acadYear = (String) cmbScoreAcadYear.getSelectedItem();
                int sem = Integer.parseInt((String) cmbScoreSemester.getSelectedItem());

                // Dynamically fetch authenticated lecturer ID via SessionManager (D-02)
                int currentLecturerId = SessionManager.getInstance().getCurrentUserId() != null
                        ? SessionManager.getInstance().getCurrentUserId() : 1;

                controller.enterScore(selectedStudentId, 0, courseCode, parts.length > 1 ? parts[1] : "",
                        acadYear, sem, cat, exam, currentLecturerId);

                showInfo("Score saved successfully!");
                fldCatScore.setText(""); fldExamScore.setText("");
            } catch (NumberFormatException ex) {
                showError("Validation Error: Please enter valid numeric scores for CAT and Exam.");
            } catch (IllegalArgumentException ex) {
                showError("Validation Error: " + ex.getMessage());
            } catch (SQLException ex) {
                showError("Database Error saving score: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Unexpected Error saving score: " + ex.getMessage());
            }
        });

        btnViewScores.addActionListener(e -> {
            if (selectedStudentId < 0) { showError("Please select a student first."); return; }
            try {
                String acadYear = (String) cmbScoreAcadYear.getSelectedItem();
                int sem = Integer.parseInt((String) cmbScoreSemester.getSelectedItem());
                List<Score> scores = controller.getScores(selectedStudentId, acadYear, sem);
                scoreModel.setRowCount(0);
                for (Score sc : scores) {
                    scoreModel.addRow(new Object[]{
                        sc.getCourseCode(), sc.getCourseTitle(),
                        String.format("%.1f", sc.getCatScore()),
                        String.format("%.1f", sc.getExamScore()),
                        String.format("%.1f", sc.getTotalScore()),
                        sc.getGrade(), sc.getGradeRemark()
                    });
                }
                if (scores.isEmpty()) showInfo("No scores found for this student in " + acadYear + " Sem " + sem + ".");
            } catch (SQLException ex) {
                showError("Database Error loading scores: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Error loading scores: " + ex.getMessage());
            }
        });

        panel.add(entryForm, BorderLayout.NORTH);
        panel.add(new JScrollPane(scoreTable), BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------
    // Data operations
    // ---------------------------------------------------------------

    private void loadStudents() {
        try {
            List<Student> students = controller.getAllStudents();
            tableModel.setRowCount(0);
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                    s.getId(), s.getRegNumber(), s.getFullName(),
                    s.getProgramme(), s.getYearOfStudy(), s.getSemester(), s.getStatus()
                });
            }
        } catch (SQLException e) {
            showError("Error loading students: " + e.getMessage());
        }
    }

    private void performSearch() {
        String kw = fldSearch.getText().trim();
        if (kw.isEmpty()) { loadStudents(); return; }
        try {
            List<Student> results = controller.searchStudents(kw);
            tableModel.setRowCount(0);
            for (Student s : results) {
                tableModel.addRow(new Object[]{
                    s.getId(), s.getRegNumber(), s.getFullName(),
                    s.getProgramme(), s.getYearOfStudy(), s.getSemester(), s.getStatus()
                });
            }
        } catch (IllegalArgumentException e) {
            showError("Validation Error: " + e.getMessage());
        } catch (SQLException e) {
            showError("Database Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Search error: " + e.getMessage());
        }
    }

    private void saveStudent() {
        try {
            Student s = controller.registerStudent(
                fldFirstName.getText(), fldLastName.getText(), fldEmail.getText(),
                fldPhone.getText(), (String) cmbGender.getSelectedItem(),
                parseDate(fldDOB.getText()), fldAddress.getText(),
                fldRegNumber.getText(), fldProgramme.getText(),
                Integer.parseInt((String) cmbYear.getSelectedItem()),
                Integer.parseInt((String) cmbSemester.getSelectedItem()),
                parseDate(fldEnrollDate.getText())
            );
            showInfo("Student registered successfully! ID: " + s.getId());
            clearForm();
            loadStudents();
        } catch (IllegalArgumentException e) {
            showError("Validation Error: " + e.getMessage());
        } catch (SQLException e) {
            showError("Database Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected Error: " + e.getMessage());
        }
    }

    private void updateStudent() {
        if (selectedStudentId < 0) { showError("Please select a student to update."); return; }
        try {
            Student s = controller.getStudentById(selectedStudentId);
            s.setFirstName(fldFirstName.getText());
            s.setLastName(fldLastName.getText());
            s.setEmail(fldEmail.getText());
            s.setPhone(fldPhone.getText());
            s.setGender((String) cmbGender.getSelectedItem());
            s.setDateOfBirth(parseDate(fldDOB.getText()));
            s.setAddress(fldAddress.getText());
            s.setRegNumber(fldRegNumber.getText());
            s.setProgramme(fldProgramme.getText());
            s.setYearOfStudy(Integer.parseInt((String) cmbYear.getSelectedItem()));
            s.setSemester(Integer.parseInt((String) cmbSemester.getSelectedItem()));
            s.setEnrollmentDate(parseDate(fldEnrollDate.getText()));
            s.setStatus((String) cmbStatus.getSelectedItem());
            controller.updateStudent(s);
            showInfo("Student updated successfully!");
            loadStudents();
        } catch (IllegalArgumentException e) {
            showError("Validation Error: " + e.getMessage());
        } catch (SQLException e) {
            showError("Database Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected Error: " + e.getMessage());
        }
    }

    private void deleteStudent() {
        if (selectedStudentId < 0) { showError("Please select a student to delete."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this student? This action cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            controller.deleteStudent(selectedStudentId);
            showInfo("Student deleted.");
            clearForm();
            loadStudents();
        } catch (IllegalArgumentException e) {
            showError("Validation Error: " + e.getMessage());
        } catch (SQLException e) {
            showError("Database Error: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected Error: " + e.getMessage());
        }
    }

    private void populateFormFromTable() {
        int row = studentTable.getSelectedRow();
        if (row < 0) return;
        selectedStudentId = (int) tableModel.getValueAt(row, 0);
        try {
            Student s = controller.getStudentById(selectedStudentId);
            if (s == null) return;
            fldFirstName.setText(s.getFirstName());
            fldLastName.setText(s.getLastName());
            fldEmail.setText(s.getEmail());
            fldPhone.setText(s.getPhone());
            fldAddress.setText(s.getAddress());
            fldRegNumber.setText(s.getRegNumber());
            fldProgramme.setText(s.getProgramme());
            fldDOB.setText(s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "");
            fldEnrollDate.setText(s.getEnrollmentDate() != null ? s.getEnrollmentDate().toString() : "");
            cmbGender.setSelectedItem(s.getGender());
            cmbYear.setSelectedItem(String.valueOf(s.getYearOfStudy()));
            cmbSemester.setSelectedItem(String.valueOf(s.getSemester()));
            cmbStatus.setSelectedItem(s.getStatus());

            // ADD THIS LINE HERE;
            refreshScoreCourseList();

        } catch (SQLException e) {
            showError("Error loading student: " + e.getMessage());
        }
    }

    private void clearForm() {
        selectedStudentId = -1;
        fldFirstName.setText(""); fldLastName.setText(""); fldEmail.setText("");
        fldPhone.setText(""); fldAddress.setText(""); fldRegNumber.setText("");
        fldProgramme.setText(""); fldDOB.setText(""); fldEnrollDate.setText("");
        cmbGender.setSelectedIndex(0); cmbYear.setSelectedIndex(0);
        cmbSemester.setSelectedIndex(0); cmbStatus.setSelectedIndex(0);
        studentTable.clearSelection();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try { 
            return LocalDate.parse(text.trim()); 
        } catch (Exception e) { 
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD"); 
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshScoreCourseList() {
        // Clear existing items so they don't double up
        cmbScoreCourse.removeAllItems();

        if (selectedStudentId < 0) return;

        try {
            String acadYear = cmbScoreAcadYear != null ? (String) cmbScoreAcadYear.getSelectedItem() : "2024/2025";
            int sem = cmbScoreSemester != null ? Integer.parseInt((String) cmbScoreSemester.getSelectedItem()) : 1;

            List<Course> courses = controller.getEnrolledCourses(selectedStudentId, acadYear, sem);

            for (Course c : courses) {
                // Add the course in the format "CS101 – Intro to Java"
                // Use a standard hyphen here
                cmbScoreCourse.addItem(c.getCourseCode() + " - " + c.getTitle());
            }
        } catch (SQLException e) {
            showError("Error loading courses for dropdown: " + e.getMessage());
        }
    }
}
