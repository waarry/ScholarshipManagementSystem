package com.scholarship.ui;

import com.scholarship.dao.ProfileDAO;
import com.scholarship.model.StudentProfile;
import com.scholarship.util.Session;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Student tab: edit personal and academic details. These are used to check scholarship eligibility. */
public class ProfilePanel extends JPanel {
    private final ProfileDAO dao = new ProfileDAO();
    private final int studentId = Session.getCurrentUser().getId();

    private final JTextField nameField = new JTextField(25);
    private final JTextField emailField = new JTextField(25);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField dobField = new JTextField(12);
    private final JTextField courseField = new JTextField(15);
    private final JComboBox<String> levelBox =
            new JComboBox<>(new String[]{"", "School", "Diploma", "Undergraduate", "Postgraduate"});
    private final JTextField percentField = new JTextField(8);
    private final JTextField incomeField = new JTextField(12);
    private final JTextArea addressArea = new JTextArea(3, 25);

    public ProfilePanel() {
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        emailField.setEditable(false);
        emailField.setText(Session.getCurrentUser().getEmail());
        addressArea.setLineWrap(true);

        int r = 0;
        addRow(form, r++, "Full name *", nameField);
        addRow(form, r++, "Email (cannot change)", emailField);
        addRow(form, r++, "Phone (10 digits)", phoneField);
        addRow(form, r++, "Date of birth (yyyy-MM-dd)", dobField);
        addRow(form, r++, "Course (e.g. BCA)", courseField);
        addRow(form, r++, "Education level", levelBox);
        addRow(form, r++, "Latest percentage (0-100)", percentField);
        addRow(form, r++, "Family income (per year)", incomeField);
        addRow(form, r++, "Address", new JScrollPane(addressArea));

        JButton save = new JButton("Save Profile");
        save.addActionListener(e -> save());
        JPanel south = new JPanel();
        south.add(save);

        add(new JScrollPane(form), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        load();
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = row;
        form.add(new JLabel(label), g);
        g.gridx = 1;
        form.add(field, g);
    }

    private void load() {
        try {
            StudentProfile p = dao.get(studentId);
            nameField.setText(Session.getCurrentUser().getFullName());
            phoneField.setText(p.getPhone() == null ? "" : p.getPhone());
            dobField.setText(p.getDateOfBirth() == null ? "" : p.getDateOfBirth().toString());
            courseField.setText(p.getCourse() == null ? "" : p.getCourse());
            levelBox.setSelectedItem(p.getEducationLevel() == null ? "" : p.getEducationLevel());
            percentField.setText(p.getPercentage() == null ? "" : String.valueOf(p.getPercentage()));
            incomeField.setText(p.getFamilyIncome() == null ? "" : String.valueOf(p.getFamilyIncome()));
            addressArea.setText(p.getAddress() == null ? "" : p.getAddress());
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { warn("Name is required."); return; }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("\\d{10}")) { warn("Phone must be exactly 10 digits."); return; }

        LocalDate dob = null;
        if (!dobField.getText().isBlank()) {
            try {
                dob = LocalDate.parse(dobField.getText().trim());
            } catch (DateTimeParseException e) { warn("Date of birth must look like 2005-04-21."); return; }
            if (!dob.isBefore(LocalDate.now())) { warn("Date of birth must be in the past."); return; }
        }

        Double percent = null;
        if (!percentField.getText().isBlank()) {
            try {
                percent = Double.parseDouble(percentField.getText().trim());
            } catch (NumberFormatException e) { warn("Percentage must be a number."); return; }
            if (percent < 0 || percent > 100) { warn("Percentage must be between 0 and 100."); return; }
        }

        Double income = null;
        if (!incomeField.getText().isBlank()) {
            try {
                income = Double.parseDouble(incomeField.getText().trim());
            } catch (NumberFormatException e) { warn("Income must be a number."); return; }
            if (income < 0) { warn("Income cannot be negative."); return; }
        }

        StudentProfile p = new StudentProfile();
        p.setStudentId(studentId);
        p.setPhone(phone);
        p.setDateOfBirth(dob);
        p.setCourse(courseField.getText().trim());
        String level = (String) levelBox.getSelectedItem();
        p.setEducationLevel(level == null || level.isEmpty() ? null : level);
        p.setPercentage(percent);
        p.setFamilyIncome(income);
        p.setAddress(addressArea.getText().trim());

        try {
            dao.save(p, name);
            JOptionPane.showMessageDialog(this, "Profile saved.");
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}