package view;

import controller.LecturerController;
import model.Course;
import model.Lecturer;
import util.AccessDeniedException;
import util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * LecturerPanel – Swing view for all lecturer-related operations.
 *
 * <p>Provides lecturer registration, search, update, deletion,
 * and assigned-course management.</p>
 */
public class LecturerPanel extends JPanel {

    private final LecturerController controller = new LecturerController();

    private JTable           lecturerTable;
    private DefaultTableModel tableModel;

    private JTextField fldFirstName, fldLastName, fldEmail, fldPhone;
    private JTextField fldAddress, fldStaffId, fldDepartment;
    private JTextField fldQualification, fldSpecialization, fldHireDate, fldDOB;
    private JComboBox<String> cmbGender, cmbStatus;
    private JTextField fldSearch;
    private JTextField fldCourseSearch;

    private int selectedLecturerId = -1;

    public LecturerPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        buildUI();
        loadLecturers();
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
        btnRefresh.addActionListener(e -> { fldSearch.setText(""); loadLecturers(); });
        fldSearch.addActionListener(e -> performSearch());

        searchBar.add(new JLabel("Search:"));
        searchBar.add(fldSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnRefresh);
        add(searchBar, BorderLayout.NORTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(560);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setLeftComponent(buildTablePanel());
        split.setRightComponent(buildFormPanel());
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        JLabel title = new JLabel("Lecturers");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.SUCCESS);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Staff ID", "Full Name", "Department", "Specialization", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        lecturerTable = new JTable(tableModel);
        lecturerTable.setRowHeight(26);
        lecturerTable.setShowGrid(false);
        lecturerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lecturerTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        lecturerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateForm();
        });

        JScrollPane scroll = new JScrollPane(lecturerTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lecturer Details", buildDetailsTab());
        tabs.addTab("Assigned Courses", buildCoursesTab());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BACKGROUND);

        JPanel form = UITheme.sectionPanel("Lecturer Information");
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        fldFirstName      = UITheme.styledField(15);
        fldLastName       = UITheme.styledField(15);
        fldEmail          = UITheme.styledField(20);
        fldPhone          = UITheme.styledField(15);
        fldAddress        = UITheme.styledField(20);
        fldStaffId        = UITheme.styledField(15);
        fldDepartment     = UITheme.styledField(20);
        fldQualification  = UITheme.styledField(20);
        fldSpecialization = UITheme.styledField(20);
        fldHireDate       = UITheme.styledField(12);
        fldDOB            = UITheme.styledField(12);
        cmbGender         = UITheme.styledCombo(new String[]{"Male","Female","Other"});
        cmbStatus         = UITheme.styledCombo(new String[]{"Active","On Leave","Retired"});

        addRow(form, gbc, 0,  "First Name *",     fldFirstName);
        addRow(form, gbc, 1,  "Last Name *",      fldLastName);
        addRow(form, gbc, 2,  "Email *",          fldEmail);
        addRow(form, gbc, 3,  "Phone",            fldPhone);
        addRow(form, gbc, 4,  "Gender *",         cmbGender);
        addRow(form, gbc, 5,  "Date of Birth",    fldDOB);
        addRow(form, gbc, 6,  "Address",          fldAddress);
        addRow(form, gbc, 7,  "Staff ID *",       fldStaffId);
        addRow(form, gbc, 8,  "Department *",     fldDepartment);
        addRow(form, gbc, 9,  "Qualification",    fldQualification);
        addRow(form, gbc, 10, "Specialization",   fldSpecialization);
        addRow(form, gbc, 11, "Hire Date *",      fldHireDate);
        addRow(form, gbc, 12, "Status",           cmbStatus);

        panel.add(new JScrollPane(form), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        btnPanel.setBackground(UITheme.BACKGROUND);

        SessionManager.Role currentRole = SessionManager.getInstance().getCurrentRole();
        boolean canManage = (currentRole == SessionManager.Role.ADMIN || currentRole == SessionManager.Role.REGISTRAR);

        JButton btnAdd    = UITheme.primaryButton("Add");
        JButton btnUpdate = UITheme.warningButton("Update");
        JButton btnDelete = UITheme.dangerButton("Delete");
        JButton btnClear  = UITheme.successButton("Clear");

        btnAdd.setEnabled(canManage);
        btnUpdate.setEnabled(canManage);
        btnDelete.setEnabled(canManage);

        btnAdd.addActionListener(e -> saveLecturer());
        btnUpdate.addActionListener(e -> updateLecturer());
        btnDelete.addActionListener(e -> deleteLecturer());
        btnClear.addActionListener(e -> clearForm());

        btnPanel.add(btnAdd); btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete); btnPanel.add(btnClear);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCoursesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.setBackground(UITheme.BACKGROUND);
        fldCourseSearch = UITheme.styledField(20);
        JButton btnView   = UITheme.primaryButton("View Courses");
        JButton btnSearch = UITheme.successButton("Search");

        String[] cols = {"Code", "Title", "Credits", "Department"};
        DefaultTableModel courseModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable courseTable = new JTable(courseModel);
        courseTable.setRowHeight(26);

        btnView.addActionListener(e -> {
            if (selectedLecturerId < 0) { showError("Please select a lecturer first."); return; }
            try {
                List<Course> courses = controller.getAssignedCourses(selectedLecturerId, "2024/2025", 1);
                courseModel.setRowCount(0);
                for (Course c : courses) {
                    courseModel.addRow(new Object[]{c.getCourseCode(), c.getTitle(), c.getCreditHours(), c.getDepartment()});
                }
                if (courses.isEmpty()) showInfo("No courses assigned to this lecturer.");
            } catch (AccessDeniedException ex) {
                showError("Access Denied: " + ex.getMessage());
            } catch (SQLException ex) { showError("Error: " + ex.getMessage()); }
        });

        btnSearch.addActionListener(e -> {
            if (selectedLecturerId < 0) { showError("Please select a lecturer first."); return; }
            String kw = fldCourseSearch.getText().trim();
            if (kw.isEmpty()) return;
            try {
                List<Course> courses = controller.searchAssignedCourses(selectedLecturerId, "2024/2025", 1, kw);
                courseModel.setRowCount(0);
                for (Course c : courses) {
                    courseModel.addRow(new Object[]{c.getCourseCode(), c.getTitle(), c.getCreditHours(), c.getDepartment()});
                }
            } catch (AccessDeniedException ex) {
                showError("Access Denied: " + ex.getMessage());
            } catch (SQLException ex) { showError("Error: " + ex.getMessage()); }
        });

        searchRow.add(new JLabel("Search courses:"));
        searchRow.add(fldCourseSearch);
        searchRow.add(btnSearch);
        searchRow.add(btnView);

        panel.add(searchRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(courseTable), BorderLayout.CENTER);
        return panel;
    }

    // ---- Data operations ----

    private void loadLecturers() {
        try {
            List<Lecturer> lecturers = controller.getAllLecturers();
            tableModel.setRowCount(0);
            for (Lecturer l : lecturers) {
                tableModel.addRow(new Object[]{
                    l.getId(), l.getStaffId(), l.getFullName(),
                    l.getDepartment(), l.getSpecialization(), l.getStatus()
                });
            }
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (SQLException e) { showError("Error loading lecturers: " + e.getMessage()); }
    }

    private void performSearch() {
        String kw = fldSearch.getText().trim();
        if (kw.isEmpty()) { loadLecturers(); return; }
        try {
            List<Lecturer> results = controller.searchLecturers(kw);
            tableModel.setRowCount(0);
            for (Lecturer l : results) {
                tableModel.addRow(new Object[]{
                    l.getId(), l.getStaffId(), l.getFullName(),
                    l.getDepartment(), l.getSpecialization(), l.getStatus()
                });
            }
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Search error: " + e.getMessage()); }
    }

    private void saveLecturer() {
        try {
            controller.addLecturer(
                fldFirstName.getText(), fldLastName.getText(), fldEmail.getText(),
                fldPhone.getText(), (String) cmbGender.getSelectedItem(),
                parseDate(fldDOB.getText()), fldAddress.getText(),
                fldStaffId.getText(), fldDepartment.getText(),
                fldQualification.getText(), fldSpecialization.getText(),
                parseDate(fldHireDate.getText())
            );
            showInfo("Lecturer added successfully!");
            clearForm(); loadLecturers();
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void updateLecturer() {
        if (selectedLecturerId < 0) { showError("Please select a lecturer to update."); return; }
        try {
            Lecturer l = controller.getLecturerById(selectedLecturerId);
            l.setFirstName(fldFirstName.getText()); l.setLastName(fldLastName.getText());
            l.setEmail(fldEmail.getText());         l.setPhone(fldPhone.getText());
            l.setGender((String) cmbGender.getSelectedItem());
            l.setDateOfBirth(parseDate(fldDOB.getText()));
            l.setAddress(fldAddress.getText());     l.setStaffId(fldStaffId.getText());
            l.setDepartment(fldDepartment.getText());
            l.setQualification(fldQualification.getText());
            l.setSpecialization(fldSpecialization.getText());
            l.setHireDate(parseDate(fldHireDate.getText()));
            l.setStatus((String) cmbStatus.getSelectedItem());
            controller.updateLecturer(l);
            showInfo("Lecturer updated!"); loadLecturers();
        } catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void deleteLecturer() {
        if (selectedLecturerId < 0) { showError("Please select a lecturer to delete."); return; }
        int c = JOptionPane.showConfirmDialog(this, "Delete this lecturer?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try { controller.deleteLecturer(selectedLecturerId); showInfo("Deleted."); clearForm(); loadLecturers(); }
        catch (AccessDeniedException e) { showError("Access Denied: " + e.getMessage()); }
        catch (Exception e) { showError("Error: " + e.getMessage()); }
    }

    private void populateForm() {
        int row = lecturerTable.getSelectedRow();
        if (row < 0) return;
        selectedLecturerId = (int) tableModel.getValueAt(row, 0);
        try {
            Lecturer l = controller.getLecturerById(selectedLecturerId);
            if (l == null) return;
            fldFirstName.setText(l.getFirstName());   fldLastName.setText(l.getLastName());
            fldEmail.setText(l.getEmail());            fldPhone.setText(l.getPhone());
            fldAddress.setText(l.getAddress());        fldStaffId.setText(l.getStaffId());
            fldDepartment.setText(l.getDepartment());  fldQualification.setText(l.getQualification());
            fldSpecialization.setText(l.getSpecialization());
            fldDOB.setText(l.getDateOfBirth() != null ? l.getDateOfBirth().toString() : "");
            fldHireDate.setText(l.getHireDate() != null ? l.getHireDate().toString() : "");
            cmbGender.setSelectedItem(l.getGender()); cmbStatus.setSelectedItem(l.getStatus());
        } catch (SQLException e) { showError("Error: " + e.getMessage()); }
    }

    private void clearForm() {
        selectedLecturerId = -1;
        fldFirstName.setText(""); fldLastName.setText(""); fldEmail.setText("");
        fldPhone.setText(""); fldAddress.setText(""); fldStaffId.setText("");
        fldDepartment.setText(""); fldQualification.setText(""); fldSpecialization.setText("");
        fldHireDate.setText(""); fldDOB.setText("");
        cmbGender.setSelectedIndex(0); cmbStatus.setSelectedIndex(0);
        lecturerTable.clearSelection();
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label + ":"); lbl.setFont(UITheme.FONT_BODY);
        p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; p.add(field, gbc);
    }

    private LocalDate parseDate(String t) {
        if (t == null || t.trim().isEmpty()) return null;
        try { return LocalDate.parse(t.trim()); } catch (Exception e) { return null; }
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
}
