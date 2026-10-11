package view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * UITheme – Centralised styling constants and factory methods for the Swing GUI.
 *
 * <p>Ensures a consistent, professional look across all views in the
 * Student Management System.</p>
 */
public class UITheme {

    // ---------------------------------------------------------------
    // Colour palette
    // ---------------------------------------------------------------
    public static final Color PRIMARY        = new Color(25,  84, 163);   // Deep blue
    public static final Color PRIMARY_DARK   = new Color(15,  55, 120);   // Darker blue
    public static final Color PRIMARY_LIGHT  = new Color(70, 130, 210);   // Lighter blue
    public static final Color ACCENT         = new Color(255, 152,   0);  // Amber
    public static final Color SUCCESS        = new Color( 56, 142,  60);  // Green
    public static final Color DANGER         = new Color(211,  47,  47);  // Red
    public static final Color WARNING        = new Color(245, 124,   0);  // Orange
    public static final Color BACKGROUND     = new Color(245, 247, 250);  // Light grey-blue
    public static final Color SURFACE        = Color.WHITE;
    public static final Color TEXT_PRIMARY   = new Color( 33,  33,  33);
    public static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    public static final Color BORDER_COLOR   = new Color(200, 210, 225);
    public static final Color TABLE_HEADER   = new Color( 37,  99, 175);
    public static final Color TABLE_ALT_ROW  = new Color(240, 245, 255);

    // ---------------------------------------------------------------
    // Fonts
    // ---------------------------------------------------------------
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO       = new Font("Consolas",  Font.PLAIN, 12);
    public static final Font FONT_BUTTON     = new Font("Segoe UI", Font.BOLD,  12);

    // ---------------------------------------------------------------
    // Dimensions
    // ---------------------------------------------------------------
    public static final int BUTTON_HEIGHT = 36;
    public static final int FIELD_HEIGHT  = 32;
    public static final Insets PADDING    = new Insets(10, 15, 10, 15);

    // ---------------------------------------------------------------
    // Factory methods
    // ---------------------------------------------------------------

    /** Creates a styled primary action button. */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_DARK); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(PRIMARY); }
        });
        return btn;
    }

    /** Creates a styled success (green) button. */
    public static JButton successButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(SUCCESS);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));
        return btn;
    }

    /** Creates a styled danger (red) button. */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));
        return btn;
    }

    /** Creates a styled warning (amber) button. */
    public static JButton warningButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, BUTTON_HEIGHT));
        return btn;
    }

    /** Creates a styled text field. */
    public static JTextField styledField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, FIELD_HEIGHT));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return field;
    }

    /** Creates a styled combo box. */
    public static <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setFont(FONT_BODY);
        combo.setBackground(SURFACE);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, FIELD_HEIGHT));
        return combo;
    }

    /** Creates a titled section panel with consistent styling. */
    public static JPanel sectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), title,
            TitledBorder.LEFT, TitledBorder.TOP, FONT_SUBTITLE, PRIMARY
        );
        panel.setBorder(BorderFactory.createCompoundBorder(
            border, BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return panel;
    }

    /** Creates a header panel with gradient-like appearance. */
    public static JPanel headerPanel(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY, getWidth(), 0, PRIMARY_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setPreferredSize(new Dimension(0, 70));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(FONT_SMALL);
        subLabel.setForeground(new Color(200, 220, 255));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subLabel);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }

    /** Applies global Look and Feel settings. */
    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default
        }
        UIManager.put("Table.font",           FONT_BODY);
        UIManager.put("TableHeader.font",     FONT_SUBTITLE);
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Label.font",           FONT_BODY);
        UIManager.put("Button.font",          FONT_BUTTON);
        UIManager.put("TextField.font",       FONT_BODY);
        UIManager.put("ComboBox.font",        FONT_BODY);
        UIManager.put("TabbedPane.font",      FONT_BODY);
        UIManager.put("Panel.background",     BACKGROUND);
    }
}
