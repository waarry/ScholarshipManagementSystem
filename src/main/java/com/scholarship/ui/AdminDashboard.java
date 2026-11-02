package com.scholarship.ui;

import com.scholarship.util.Session;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {
    public AdminDashboard() {
        super("Scholarship Management System - Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);

        JLabel welcome = new JLabel("Welcome, " + Session.getCurrentUser().getFullName());
        welcome.setFont(new Font("SansSerif", Font.BOLD, 16));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        top.add(welcome, BorderLayout.WEST);
        top.add(logout, BorderLayout.EAST);

        ScholarshipPanel scholarships = new ScholarshipPanel();
        AdminApplicationsPanel applications = new AdminApplicationsPanel();
        StudentsPanel students = new StudentsPanel();
        ReportsPanel reports = new ReportsPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Scholarships", scholarships);
        tabs.addTab("Applications", applications);
        tabs.addTab("Students", students);
        tabs.addTab("Reports", reports);
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c == applications) applications.refresh();
            else if (c == students) students.refresh();
            else if (c == reports) reports.refresh();
        });

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}