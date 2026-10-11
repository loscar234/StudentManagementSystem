package view;

import controller.StudentController;
import model.Score;
import model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ResultSlipPanel – Swing view for generating and displaying student result slips.
 *
 * <p>Displays a formatted result slip showing course codes, titles, CAT scores,
 * exam scores, totals, grades, and computed averages. Also saves the slip to
 * the file system via the controller.</p>
 */
public class ResultSlipPanel extends JPanel {

    private final StudentController controller = new StudentController();

    private JTextField fldRegNumber;
    private JTable     resultTable;
    private DefaultTableModel tableModel;

    // Summary labels
    private JLabel lblStudentName, lblRegNo, lblProgramme, lblYear;
    private JLabel lblAvgScore, lblAvgGrade, lblGenerated;

    public ResultSlipPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BACKGROUND);
        buildUI();
    }

    private void buildUI() {
        // ---- Search bar ----
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchBar.setBackground(UITheme.SURFACE);
        searchBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        JLabel lbl = new JLabel("Registration Number:");
        lbl.setFont(UITheme.FONT_BODY);

        fldRegNumber = UITheme.styledField(20);
        fldRegNumber.setToolTipText("e.g. CS/001/2023");

        JButton btnGenerate = UITheme.primaryButton("Generate Slip");
        JButton btnPrint    = UITheme.successButton("Save to File");

        btnGenerate.addActionListener(e -> generateSlip());
        btnPrint.addActionListener(e -> saveSlip());
        fldRegNumber.addActionListener(e -> generateSlip());

        searchBar.add(lbl);
        searchBar.add(fldRegNumber);
        searchBar.add(btnGenerate);
        searchBar.add(btnPrint);
        add(searchBar, BorderLayout.NORTH);

        // ---- Main content ----
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Student info panel
        content.add(buildStudentInfoPanel(), BorderLayout.NORTH);

        // Result table
        String[] cols = {"Course Code", "Course Title", "CAT (30)", "Exam (70)", "Total (100)", "Grade", "Remark"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(28);
        resultTable.setShowGrid(true);
        resultTable.setGridColor(UITheme.BORDER_COLOR);
        resultTable.setFont(UITheme.FONT_BODY);
        resultTable.getTableHeader().setFont(UITheme.FONT_SUBTITLE);
        resultTable.getTableHeader().setBackground(UITheme.TABLE_HEADER);
        resultTable.getTableHeader().setForeground(Color.WHITE);

        // Grade-coloured renderer
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    Object grade = t.getModel().getValueAt(row, 5);
                    if ("A".equals(grade))      setBackground(new Color(232, 245, 233));
                    else if ("B".equals(grade)) setBackground(new Color(232, 240, 254));
                    else if ("C".equals(grade)) setBackground(new Color(255, 253, 231));
                    else if ("D".equals(grade)) setBackground(new Color(255, 243, 224));
                    else if ("F".equals(grade)) setBackground(new Color(255, 235, 238));
                    else setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.TABLE_ALT_ROW);
                }
                setHorizontalAlignment(col >= 2 && col <= 4 ? CENTER : LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        resultTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(resultTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        content.add(scroll, BorderLayout.CENTER);

        // Summary footer
        content.add(buildSummaryPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // ---- Grade key legend ----
        add(buildLegend(), BorderLayout.SOUTH);
    }

    private JPanel buildStudentInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 5));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        lblStudentName = infoLabel("–");
        lblRegNo       = infoLabel("–");
        lblProgramme   = infoLabel("–");
        lblYear        = infoLabel("–");
        lblGenerated   = infoLabel("–");

        panel.add(captionLabel("Student Name:"));  panel.add(lblStudentName);
        panel.add(captionLabel("Reg. Number:"));   panel.add(lblRegNo);
        panel.add(captionLabel("Programme:"));     panel.add(lblProgramme);
        panel.add(captionLabel("Year / Semester:")); panel.add(lblYear);

        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 8));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        lblAvgScore = new JLabel("Average Score: –");
        lblAvgScore.setFont(UITheme.FONT_SUBTITLE);
        lblAvgScore.setForeground(UITheme.PRIMARY);

        lblAvgGrade = new JLabel("Average Grade: –");
        lblAvgGrade.setFont(UITheme.FONT_SUBTITLE);
        lblAvgGrade.setForeground(UITheme.SUCCESS);

        lblGenerated = new JLabel("Generated: –");
        lblGenerated.setFont(UITheme.FONT_SMALL);
        lblGenerated.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(lblGenerated);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblAvgScore);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(lblAvgGrade);
        return panel;
    }

    private JPanel buildLegend() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 6));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        panel.add(legendItem("A: 70–100 (Distinction)", new Color(232, 245, 233)));
        panel.add(legendItem("B: 60–69 (Credit)",       new Color(232, 240, 254)));
        panel.add(legendItem("C: 50–59 (Pass)",         new Color(255, 253, 231)));
        panel.add(legendItem("D: 40–49 (Marginal Pass)",new Color(255, 243, 224)));
        panel.add(legendItem("F: 0–39 (Fail)",          new Color(255, 235, 238)));
        return panel;
    }

    private JLabel legendItem(String text, Color bg) {
        JLabel lbl = new JLabel("  " + text + "  ");
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        return lbl;
    }

    // ---- Data operations ----

    private void generateSlip() {
        String reg = fldRegNumber.getText().trim();
        if (reg.isEmpty()) { showError("Please enter a registration number."); return; }

        try {
            Student student = controller.getStudentByRegNumber(reg);
            if (student == null) { showError("Student not found: " + reg); return; }

            List<Score> scores = controller.getResultSlipData(reg);
            if (scores.isEmpty()) {
                showInfo("No scores found for student: " + reg);
                return;
            }

            // Populate student info
            lblStudentName.setText(student.getFullName());
            lblRegNo.setText(student.getRegNumber());
            lblProgramme.setText(student.getProgramme());
            lblYear.setText("Year " + student.getYearOfStudy() + ", Semester " + student.getSemester());

            // Populate table
            tableModel.setRowCount(0);
            double totalSum = 0;
            for (Score sc : scores) {
                tableModel.addRow(new Object[]{
                    sc.getCourseCode(),
                    sc.getCourseTitle(),
                    String.format("%.1f", sc.getCatScore()),
                    String.format("%.1f", sc.getExamScore()),
                    String.format("%.1f", sc.getTotalScore()),
                    sc.getGrade(),
                    sc.getGradeRemark()
                });
                totalSum += sc.getTotalScore();
            }

            // Compute averages
            double avg = scores.isEmpty() ? 0 : totalSum / scores.size();
            String avgGrade = computeAverageGrade(avg);

            lblAvgScore.setText(String.format("Average Score: %.2f / 100", avg));
            lblAvgGrade.setText("Average Grade: " + avgGrade);
            lblGenerated.setText("Generated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void saveSlip() {
        String reg = fldRegNumber.getText().trim();
        if (reg.isEmpty()) { showError("Please generate a slip first."); return; }
        try {
            controller.generateResultSlip(reg);
            showInfo("Result slip saved to file: data/result_slip_" + reg.replace("/", "_") + ".txt");
        } catch (Exception e) {
            showError("Error saving slip: " + e.getMessage());
        }
    }

    private String computeAverageGrade(double avg) {
        if (avg >= 70) return "A";
        if (avg >= 60) return "B";
        if (avg >= 50) return "C";
        if (avg >= 40) return "D";
        return "F";
    }

    private JLabel captionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        return lbl;
    }

    private JLabel infoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
}
