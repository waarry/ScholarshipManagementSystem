package com.scholarship.ui;

import com.scholarship.dao.UserDAO;
import com.scholarship.model.User;
import com.scholarship.util.Session;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginFrame() {
        super("Scholarship Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Scholarship Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 5, 10));
        JLabel note = new JLabel("Academic demo project - not connected to any real scholarship portal", SwingConstants.CENTER);
        note.setFont(new Font("SansSerif", Font.ITALIC, 11));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.add(title);
        header.add(note);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Email:"), g);
        g.gridx = 1; form.add(emailField, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
        g.gridx = 1; form.add(passwordField, g);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Student Registration");
        JPanel buttons = new JPanel();
        buttons.add(loginBtn);
        buttons.add(registerBtn);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> new RegisterDialog(this).setVisible(true));
        getRootPane().setDefaultButton(loginBtn);

        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email and password.");
            return;
        }
        try {
            User user = new UserDAO().authenticate(email, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid email or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Session.login(user);
            dispose();
            if (user.isAdmin()) new AdminDashboard().setVisible(true);
            else new StudentDashboard().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}