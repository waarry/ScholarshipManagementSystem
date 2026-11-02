package com.scholarship.ui;

import com.scholarship.dao.UserDAO;
import com.scholarship.util.Validator;

import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);

    public RegisterDialog(Frame parent) {
        super(parent, "Student Registration", true);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Full name:", "Email:", "Password:", "Confirm password:"};
        JComponent[] fields = {nameField, emailField, passField, confirmField};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; form.add(new JLabel(labels[i]), g);
            g.gridx = 1; form.add(fields[i], g);
        }
        g.gridx = 1; g.gridy = labels.length;
        form.add(new JLabel("<html><small>Min 8 characters, with letters and digits</small></html>"), g);

        JButton save = new JButton("Register");
        JPanel south = new JPanel();
        south.add(save);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        save.addActionListener(e -> register());

        pack();
        setLocationRelativeTo(parent);
    }

    private void register() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pw = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (name.isEmpty()) { warn("Name is required."); return; }
        if (!Validator.isValidEmail(email)) { warn("Enter a valid email address."); return; }
        if (!Validator.isStrongPassword(pw)) { warn("Password must be 8+ characters with letters and digits."); return; }
        if (!pw.equals(confirm)) { warn("Passwords do not match."); return; }

        try {
            if (new UserDAO().registerStudent(name, email, pw)) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can log in now.");
                dispose();
            } else {
                warn("This email is already registered.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Check your input", JOptionPane.WARNING_MESSAGE);
    }
}