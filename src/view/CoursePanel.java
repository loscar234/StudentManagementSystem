package view;

import controller.CourseController;
import model.Course;
import util.AccessDeniedException;
import util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * CoursePanel – Swing view for course management.
 *
 * <p>Provides CRUD operations for courses and allocation to students/lecturers.</p>
 */
public class CoursePanel extends JPanel {

    private final CourseController controller = new CourseController();

    private JTable            courseTable;
    private DefaultTableModel tableModel;

    private JTextField fldCode, fldTitle, fldDescription, fldCreditHours;
    private JTextField fldDepartment, fldLevel;
    private JComboBox<String> cmbSemester;
    private JTextField fldSearch;

    private int selectedCourseId = -1;

    public CoursePanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        buildUI();
        loadCourses();
    }

    private void buildUI() {
        // Search bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchBar.setBackground(UITheme.SURFACE);
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        fldSearch = UITheme.styledField(25);
        JButton btnSearch  = UITheme.primaryButton("Search");
        JButton btnRefresh = UITheme.successButton("Refresh");

        btnSearch.addActionListener(e -> performSearch());
        btnRefresh.addActionListener(e -> { fldSearch.setText(""); loadCourses(); });
        fldSearch.addActionListener(e -> performSearch());

        searchBar.add(new JLabel("Search by code or title:"));
        searchBar.add(fldSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnRefresh);
        add(searchBar, BorderLayout.NORTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(600);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        split.setLeftComponent(buildTablePanel());
        split.setRightComponent(buildFormPanel());
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JLabel title = new JLabel("Course Catalogue");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.WARNING);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Code", "Title", "Credits", "Department", "Level", "Sem"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        courseTable = new JTable(tableModel);
        courseTable.setRowHeight(26);
        courseTable.setShowGrid(false);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        courseTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        courseTable.getColumnModel().getColumn(5).setPreferredWidth(50);
        courseTable.getColumnModel().getColumn(6).setPreferredWidth(40);

        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateForm();
        });

        JScrollPane scroll = new JScrollPane(courseTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JPanel form = UITheme.sectionPanel("Course Details");
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        fldCode        = UITheme.styledField(12);
        fldTitle       = UITheme.styledField(20);
        fldDescription = UITheme.styledField(20);
        fldCreditHours = UITheme.styledField(5);
        fldDepartment  = UITheme.styledField(20);
        fldLevel       = UITheme.styledField(5);
        cmbSemester    = UITheme.styledCombo(new String[]{"1","2"});

        fldCode.setToolTipText("e.g. CS101");
        fldLevel.setToolTipText("e.g. 100, 200, 300, 400");

        addRow(form, gbc, 0, "Course Code *",  fldCode);
        addRow(form, gbc, 1, "Title *",        fldTitle);
        addRow(form, gbc, 2, "Description",    fldDescription);
        addRow(form, gbc, 3, "Credit Hours *", fldCreditHours);
        addRow(form, gbc, 4, "Department *",   fldDepartment);
        addRow(form, gbc, 5, "Level",          fldLevel);
        addRow(form, gbc, 6, "Semester *",     cmbSemester);

        // Allocation section
        JPanel allocPanel = UITheme.sectionPanel("Course Allocation");
        allocPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        JTextField fldStudentId  = UITheme.styledField(10);
        JTextField fldLecturerId = UITheme.styledField(10);
        JTextField fldAcadYear   = UITheme.styledField(10);
        fldAcadYear.setText("2024/2025");

        addRow(allocPanel, gbc2, 0, "Student ID",   fldStudentId);
        addRow(allocPanel, gbc2, 1, "Lecturer ID",  fldLecturerId);
        addRow(allocPanel, gbc2, 2, "Academic Year",fldAcadYear);

        SessionManager.Role currentRole = SessionManager.getInstance().getCurrentRole();
        boolean canManage = (currentRole == SessionManager.Role.ADMIN || currentRole == SessionManager.Role.REGISTRAR);

        JButton btnAllocStudent  = UITheme.primaryButton("Alloc → Student");
        JButton btnAllocLecturer = UITheme.successButton("Alloc → Lecturer");
        btnAllocStudent.setEnabled(canManage);
        btnAllocLecturer.setEnabled(canManage);

        btnAllocStudent.addActionListener(e -> {
            if (selectedCourseId < 0) { showError("Select a course first."); return; }
            try {
                int sid = Integer.parseInt(fldStudentId.getText().trim());
                controller.allocateCourseToStudent(sid, selectedCourseId,
                        fldAcadYear.getText().trim(), Integer.parseInt((String) cmbSemester.getSelectedItem()));
                showInfo("Course allocated to student successfully!");
            } catch (AccessDeniedException ex) {
                showError("Access Denied: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                showError("Enter a valid Student ID.");
            } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
        });

        btnAllocLecturer.addActionListener(e -> {
            if (selectedCourseId < 0) { showError("Select a course first."); return; }
            try {
                int lid = Integer.parseInt(fldLecturerId.getText().trim());
                controller.allocateCourseToLecturer(lid, selectedCourseId,
                        fldAcadYear.getText().trim(), Integer.parseInt((String) cmbSemester.getSelectedItem()));
                showInfo("Course allocated to lecturer successfully!");
            } catch (AccessDeniedException ex) {
                showError("Access Denied: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                showError("Enter a valid Lecturer ID.");
            } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
        });

        gbc2.gridx = 0; gbc2.gridy = 3; gbc2.gridwidth = 2;
        JPanel allocBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        allocBtns.setBackground(UITheme.SURFACE);
        allocBtns.add(btnAllocStudent); allocBtns.add(btnAllocLecturer);
        allocPanel.add(allocBtns, gbc2);

        JPanel formWrapper = new JPanel(new BorderLayout(0, 10));
        formWrapper.setBackground(UITheme.BACKGROUND);
        formWrapper.add(form, BorderLayout.NORTH);
        formWrapper.add(allocPanel, BorderLayout.CENTER);

        panel.add(new JScrollPane(formWrapper), BorderLayout.CENTER);

        // Action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        btnPanel.setBackground(UITheme.BACKGROUND);
        JButton btnAdd    = UITheme.primaryButton("Add");
        JButton btnUpdate = UITheme.warningButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear  = UITheme.successButton("Clear");

        btnAdd.setEnabled(canManage);
        btnUpdate.setEnabled(canManage);
        btnDelete.setEnabled(canManage);

        btnAdd.addActionListener(e -> saveCourse());
        btnUpdate.addActionListener(e -> updateCourse());
        btnDelete.addActionListener(e -> deleteCourse());
        btnClear.addActionListener(e -> clearForm());

        btnPanel.add(btnAdd); btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete); btnPanel.add(btnClear);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ---- Data operations ----

    private void loadCourses() {
        try {
            List<Course> courses = controller.getAllCourses();
            tableModel.setRowCount(0);
            for (Course c : courses) {
                tableModel.addRow(new Object[]{
                    c.getId(), c.getCourseCode(), c.getTitle(),
                    c.getCreditHours(), c.getDepartment(), c.getLevel(), c.getSemester()
                });
            }
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (SQLException e) { showError("Error loading courses: " + e.getMessage()); }
    }

    private void performSearch() {
        String kw = fldSearch.getText().trim();
        if (kw.isEmpty()) { loadCourses(); return; }
        try {
            List<Course> results = controller.searchCourses(kw);
            tableModel.setRowCount(0);
            for (Course c : results) {
                tableModel.addRow(new Object[]{
                    c.getId(), c.getCourseCode(), c.getTitle(),
                    c.getCreditHours(), c.getDepartment(), c.getLevel(), c.getSemester()
                });
            }
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Search error: " + e.getMessage()); }
    }

    private void saveCourse() {
        try {
            int credits = Integer.parseInt(fldCreditHours.getText().trim());
            int level   = fldLevel.getText().trim().isEmpty() ? 100 : Integer.parseInt(fldLevel.getText().trim());
            int sem     = Integer.parseInt((String) cmbSemester.getSelectedItem());
            controller.addCourse(fldCode.getText(), fldTitle.getText(), fldDescription.getText(),
                    credits, fldDepartment.getText(), level, sem);
            showInfo("Course added!"); clearForm(); loadCourses();
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void updateCourse() {
        if (selectedCourseId < 0) { showError("Select a course to update."); return; }
        try {
            Course c = controller.getCourseById(selectedCourseId);
            c.setCourseCode(fldCode.getText()); c.setTitle(fldTitle.getText());
            c.setDescription(fldDescription.getText());
            c.setCreditHours(Integer.parseInt(fldCreditHours.getText().trim()));
            c.setDepartment(fldDepartment.getText());
            c.setLevel(fldLevel.getText().trim().isEmpty() ? 100 : Integer.parseInt(fldLevel.getText().trim()));
            c.setSemester(Integer.parseInt((String) cmbSemester.getSelectedItem()));
            controller.updateCourse(c);
            showInfo("Course updated!"); loadCourses();
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void deleteCourse() {
        if (selectedCourseId < 0) { showError("Select a course to delete."); return; }
        int conf = JOptionPane.showConfirmDialog(this, "Delete this course?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try { controller.deleteCourse(selectedCourseId); showInfo("Deleted."); clearForm(); loadCourses(); }
        catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void populateForm() {
        int row = courseTable.getSelectedRow();
        if (row < 0) return;
        selectedCourseId = (int) tableModel.getValueAt(row, 0);
        try {
            Course c = controller.getCourseById(selectedCourseId);
            if (c == null) return;
            fldCode.setText(c.getCourseCode());
            fldTitle.setText(c.getTitle());
            fldDescription.setText(c.getDescription() != null ? c.getDescription() : "");
            fldCreditHours.setText(String.valueOf(c.getCreditHours()));
            fldDepartment.setText(c.getDepartment());
            fldLevel.setText(String.valueOf(c.getLevel()));
            cmbSemester.setSelectedItem(String.valueOf(c.getSemester()));
        } catch (SQLException e) { showError("Error: " + e.getMessage()); }
    }

    private void clearForm() {
        selectedCourseId = -1;
        fldCode.setText(""); fldTitle.setText(""); fldDescription.setText("");
        fldCreditHours.setText(""); fldDepartment.setText(""); fldLevel.setText("");
        cmbSemester.setSelectedIndex(0); courseTable.clearSelection();
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label + ":"); lbl.setFont(UITheme.FONT_BODY); p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65; p.add(field, gbc);
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
}
