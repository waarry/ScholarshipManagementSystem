package com.scholarship.ui;

import com.scholarship.util.Session;

import javax.swing.*;
import java.awt.*;

public class StudentDashboard extends JFrame {
    public StudentDashboard() {
        super("Scholarship Management System - Student");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 620);
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

        BrowsePanel browse = new BrowsePanel();
        MyApplicationsPanel myApps = new MyApplicationsPanel();
        ProfilePanel profile = new ProfilePanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Browse Scholarships", browse);
        tabs.addTab("My Applications", myApps);
        tabs.addTab("My Profile", profile);
        // reload the lists every time the student switches tabs
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c == browse) browse.refresh();
            else if (c == myApps) myApps.refresh();
        });

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}