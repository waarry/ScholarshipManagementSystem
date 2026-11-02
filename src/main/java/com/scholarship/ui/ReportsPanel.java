package com.scholarship.ui;

import com.scholarship.dao.ReportDAO;
import com.scholarship.dao.StudentDAO;
import com.scholarship.util.CsvExporter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin tab: summary numbers, three reports, and CSV export. */
public class ReportsPanel extends JPanel {
    private static final String[] STUDENT_HEADERS = {"ID", "Name", "Email", "Phone", "Course", "Level",
            "Percentage", "Family income", "Applications", "Joined"};

    private final ReportDAO reportDAO = new ReportDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private final JPanel cards = new JPanel(new GridLayout(2, 4, 8, 8));
    private final JComboBox<String> reportBox = new JComboBox<>(
            new String[]{"Scholarship summary", "Applications (detailed)", "Student list"});
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[]{"All", "SUBMITTED", "UNDER_REVIEW", "DOCUMENTS_REQUESTED", "APPROVED", "REJECTED"});
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel infoLabel = new JLabel("Choose a report and click Generate.");

    private String[] currentHeaders = null;
    private List<String[]> currentRows = new ArrayList<>();

    public ReportsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Report:"));
        controls.add(reportBox);
        controls.add(new JLabel("Status:"));
        controls.add(statusBox);
        JButton generateBtn = new JButton("Generate");
        JButton exportBtn = new JButton("Export to CSV");
        controls.add(generateBtn);
        controls.add(exportBtn);

        // the status filter only makes sense for the detailed report
        statusBox.setEnabled(false);
        reportBox.addActionListener(e -> statusBox.setEnabled(reportBox.getSelectedIndex() == 1));
        generateBtn.addActionListener(e -> generate());
        exportBtn.addActionListener(e -> exportCsv());

        JPanel top = new JPanel(new BorderLayout(5, 8));
        top.add(cards, BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(infoLabel, BorderLayout.SOUTH);
        refresh();
    }

    /** Reloads the summary cards, and the report on screen if there is one. */
    public void refresh() {
        try {
            cards.removeAll();
            for (Map.Entry<String, String> e : reportDAO.overview().entrySet()) {
                JLabel card = new JLabel("<html><div style='text-align:center'><span style='font-size:18px'><b>"
                        + e.getValue() + "</b></span><br>" + e.getKey() + "</div></html>", SwingConstants.CENTER);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                cards.add(card);
            }
            cards.revalidate();
            cards.repaint();
            if (currentHeaders != null) generate();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void generate() {
        try {
            switch (reportBox.getSelectedIndex()) {
                case 0 -> {
                    currentHeaders = ReportDAO.SUMMARY_HEADERS;
                    currentRows = reportDAO.scholarshipSummary();
                }
                case 1 -> {
                    currentHeaders = ReportDAO.DETAIL_HEADERS;
                    String status = (String) statusBox.getSelectedItem();
                    currentRows = reportDAO.applicationDetails("All".equals(status) ? null : status);
                }
                default -> {
                    currentHeaders = STUDENT_HEADERS;
                    currentRows = studentDAO.findAll();
                }
            }
            Object[][] data = new Object[currentRows.size()][];
            for (int i = 0; i < data.length; i++) data[i] = currentRows.get(i);
            model.setDataVector(data, currentHeaders);
            infoLabel.setText(reportBox.getSelectedItem() + ": " + currentRows.size() + " rows. "
                    + "Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void exportCsv() {
        if (currentHeaders == null) {
            JOptionPane.showMessageDialog(this, "Generate a report first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        chooser.setSelectedFile(new File("report_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        if (f.exists()) {
            int ok = JOptionPane.showConfirmDialog(this, "File already exists. Replace it?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
        }
        try {
            CsvExporter.write(f.toPath(), currentHeaders, currentRows);
            JOptionPane.showMessageDialog(this, "Exported " + currentRows.size() + " rows to:\n" + f.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}