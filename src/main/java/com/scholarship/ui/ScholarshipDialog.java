package com.scholarship.ui;

import com.scholarship.dao.ScholarshipDAO;
import com.scholarship.model.Scholarship;
import com.scholarship.util.Session;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Form used for both adding a new scholarship and editing an existing one. */
public class ScholarshipDialog extends JDialog {
    private final Scholarship existing;   // null when adding
    private boolean saved = false;

    private final JTextField nameField = new JTextField(28);
    private final JTextField sponsorField = new JTextField(28);
    private final JTextArea descArea = new JTextArea(3, 28);
    private final JTextArea eligibilityArea = new JTextArea(3, 28);
    private final JTextField minPercentField = new JTextField(10);
    private final JTextField incomeField = new JTextField(10);
    private final JTextField courseField = new JTextField(15);
    private final JComboBox<String> levelBox =
            new JComboBox<>(new String[]{"Any", "School", "Diploma", "Undergraduate", "Postgraduate"});
    private final JTextField amountField = new JTextField(10);
    private final JTextField openField = new JTextField(10);
    private final JTextField closeField = new JTextField(10);
    private final JTextField docsField = new JTextField(28);

    public ScholarshipDialog(Frame parent, Scholarship existing) {
        super(parent, existing == null ? "Add Scholarship" : "Edit Scholarship", true);
        this.existing = existing;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        int r = 0;
        addRow(form, r++, "Name *", nameField);
        addRow(form, r++, "Sponsor / Organization", sponsorField);
        addRow(form, r++, "Description", new JScrollPane(descArea));
        addRow(form, r++, "Eligibility criteria", new JScrollPane(eligibilityArea));
        addRow(form, r++, "Minimum % (optional)", minPercentField);
        addRow(form, r++, "Income limit (optional)", incomeField);
        addRow(form, r++, "Eligible course (e.g. BCA or Any)", courseField);
        addRow(form, r++, "Education level", levelBox);
        addRow(form, r++, "Amount *", amountField);
        addRow(form, r++, "Opening date * (yyyy-MM-dd)", openField);
        addRow(form, r++, "Closing date * (yyyy-MM-dd)", closeField);
        addRow(form, r++, "Required documents (comma separated)", docsField);

        descArea.setLineWrap(true);
        eligibilityArea.setLineWrap(true);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(cancelBtn);
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        if (existing != null) loadExisting();
        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isSaved() { return saved; }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = row;
        form.add(new JLabel(label), g);
        g.gridx = 1;
        form.add(field, g);
    }

    private void loadExisting() {
        nameField.setText(existing.getName());
        sponsorField.setText(existing.getSponsor());
        descArea.setText(existing.getDescription());
        eligibilityArea.setText(existing.getEligibility());
        if (existing.getMinPercentage() != null) minPercentField.setText(String.valueOf(existing.getMinPercentage()));
        if (existing.getIncomeLimit() != null) incomeField.setText(String.valueOf(existing.getIncomeLimit()));
        courseField.setText(existing.getEligibleCourse());
        levelBox.setSelectedItem(existing.getEducationLevel());
        amountField.setText(String.valueOf(existing.getAmount()));
        openField.setText(existing.getOpenDate().toString());
        closeField.setText(existing.getCloseDate().toString());
        docsField.setText(String.join(", ", existing.getRequiredDocs()));
    }

    private void save() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { warn("Scholarship name is required."); return; }

            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim());
            } catch (NumberFormatException e) { warn("Amount must be a number."); return; }
            if (amount <= 0) { warn("Amount must be greater than 0."); return; }

            Double minPercent = parseOptional(minPercentField.getText(), "Minimum %");
            if (minPercent == null && !minPercentField.getText().isBlank()) return;
            if (minPercent != null && (minPercent < 0 || minPercent > 100)) { warn("Minimum % must be between 0 and 100."); return; }

            Double income = parseOptional(incomeField.getText(), "Income limit");
            if (income == null && !incomeField.getText().isBlank()) return;
            if (income != null && income < 0) { warn("Income limit cannot be negative."); return; }

            LocalDate open, close;
            try {
                open = LocalDate.parse(openField.getText().trim());
                close = LocalDate.parse(closeField.getText().trim());
            } catch (DateTimeParseException e) { warn("Dates must look like 2026-12-31."); return; }
            if (close.isBefore(open)) { warn("Closing date cannot be before opening date."); return; }

            Scholarship s = (existing == null) ? new Scholarship() : existing;
            s.setName(name);
            s.setSponsor(sponsorField.getText().trim());
            s.setDescription(descArea.getText().trim());
            s.setEligibility(eligibilityArea.getText().trim());
            s.setMinPercentage(minPercent);
            s.setIncomeLimit(income);
            String course = courseField.getText().trim();
            s.setEligibleCourse(course.isEmpty() ? "Any" : course);
            s.setEducationLevel((String) levelBox.getSelectedItem());
            s.setAmount(amount);
            s.setOpenDate(open);
            s.setCloseDate(close);

            List<String> docs = new ArrayList<>();
            for (String d : docsField.getText().split(",")) {
                if (!d.trim().isEmpty()) docs.add(d.trim());
            }
            s.setRequiredDocs(docs);

            ScholarshipDAO dao = new ScholarshipDAO();
            if (existing == null) dao.insert(s, Session.getCurrentUser().getId());
            else dao.update(s);

            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Returns null if blank (no rule). If the text is not a number, shows a warning and returns null. */
    private Double parseOptional(String text, String fieldName) {
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            warn(fieldName + " must be a number or left empty.");
            return null;
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Check your input", JOptionPane.WARNING_MESSAGE);
    }
}