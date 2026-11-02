package com.scholarship.ui;

import com.scholarship.dao.ApplicationDAO;
import com.scholarship.dao.ProfileDAO;
import com.scholarship.dao.ScholarshipDAO;
import com.scholarship.model.Application;
import com.scholarship.model.Scholarship;
import com.scholarship.util.EligibilityChecker;
import com.scholarship.util.Session;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Student tab: published scholarships that are open today. */
public class BrowsePanel extends JPanel {
    private final ScholarshipDAO scholarshipDAO = new ScholarshipDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final int studentId = Session.getCurrentUser().getId();
    private List<Scholarship> data = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Sponsor", "Amount", "Level", "Closes", "Days left"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> levelFilter =
            new JComboBox<>(new String[]{"All", "School", "Diploma", "Undergraduate", "Postgraduate"});

    public BrowsePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(new JLabel("Education level:"));
        top.add(levelFilter);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        levelFilter.addActionListener(e -> applyFilter());

        JButton detailsBtn = new JButton("View Details & Eligibility");
        JButton applyBtn = new JButton("Apply / Continue Application");
        JButton refreshBtn = new JButton("Refresh");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(detailsBtn);
        bottom.add(applyBtn);
        bottom.add(refreshBtn);
        detailsBtn.addActionListener(e -> showDetails());
        applyBtn.addActionListener(e -> apply());
        refreshBtn.addActionListener(e -> refresh());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        try {
            data = scholarshipDAO.findAvailable();
            model.setRowCount(0);
            for (Scholarship s : data) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), s.getCloseDate());
                model.addRow(new Object[]{s.getId(), s.getName(), s.getSponsor(),
                        String.format("%.2f", s.getAmount()), s.getEducationLevel(), s.getCloseDate(), days});
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2));
        String level = (String) levelFilter.getSelectedItem();
        if (!"All".equals(level)) filters.add(RowFilter.regexFilter("^(" + level + "|Any)$", 4));
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private Scholarship selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a scholarship first.");
            return null;
        }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        for (Scholarship s : data) if (s.getId() == id) return s;
        return null;
    }

    private void showDetails() {
        Scholarship s = selected();
        if (s == null) return;
        try {
            List<String> problems = EligibilityChecker.check(s, profileDAO.get(studentId));
            String eligibility = problems.isEmpty()
                    ? "YOU ARE ELIGIBLE for this scholarship."
                    : "NOT ELIGIBLE YET:\n- " + String.join("\n- ", problems);
            String text = s.getName() + "\nSponsor: " + s.getSponsor()
                    + "\nAmount: " + String.format("%.2f", s.getAmount())
                    + "\nApply between: " + s.getOpenDate() + " and " + s.getCloseDate()
                    + "\n\nDescription:\n" + s.getDescription()
                    + "\n\nEligibility criteria:\n" + s.getEligibility()
                    + "\nMinimum %: " + (s.getMinPercentage() == null ? "No rule" : s.getMinPercentage())
                    + "\nIncome limit: " + (s.getIncomeLimit() == null ? "No rule" : s.getIncomeLimit())
                    + "\nCourse: " + s.getEligibleCourse() + "   |   Level: " + s.getEducationLevel()
                    + "\n\nRequired documents: "
                    + (s.getRequiredDocs().isEmpty() ? "None" : String.join(", ", s.getRequiredDocs()))
                    + "\n\n--- Your eligibility ---\n" + eligibility;
            JTextArea area = new JTextArea(text, 20, 45);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Scholarship Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void apply() {
        Scholarship s = selected();
        if (s == null) return;
        try {
            Application existing = appDAO.findForStudent(studentId, s.getId());
            if (existing == null) {
                List<String> problems = EligibilityChecker.check(s, profileDAO.get(studentId));
                if (!problems.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "You are not eligible yet:\n- " + String.join("\n- ", problems)
                                    + "\n\nUpdate your details in the My Profile tab if they are out of date.",
                            "Not eligible", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new ApplicationDialog(parent, s, existing).setVisible(true);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}