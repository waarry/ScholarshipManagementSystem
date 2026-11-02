package com.scholarship.ui;

import com.scholarship.dao.ApplicationDAO;
import com.scholarship.dao.ScholarshipDAO;
import com.scholarship.model.Application;
import com.scholarship.model.Scholarship;
import com.scholarship.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Student tab: all of my applications with their status. */
public class MyApplicationsPanel extends JPanel {
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final ScholarshipDAO scholarshipDAO = new ScholarshipDAO();
    private final int studentId = Session.getCurrentUser().getId();
    private List<Application> data = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"App ID", "Scholarship", "Status", "Submitted", "Last updated", "Deadline"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public MyApplicationsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton openBtn = new JButton("Open / View Status & Feedback");
        JButton refreshBtn = new JButton("Refresh");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(openBtn);
        bottom.add(refreshBtn);
        openBtn.addActionListener(e -> openSelected());
        refreshBtn.addActionListener(e -> refresh());

        add(new JLabel("Drafts can be edited and submitted. If documents are requested, open the application to respond."),
                BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        try {
            data = appDAO.findByStudent(studentId);
            model.setRowCount(0);
            for (Application a : data) {
                model.addRow(new Object[]{a.getId(), a.getScholarshipName(), a.getStatus(),
                        a.getSubmittedAt(), a.getUpdatedAt(), a.getCloseDate()});
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void openSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an application first.");
            return;
        }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        try {
            Application app = appDAO.findByIdForStudent(id, studentId);   // checks the owner
            if (app == null) return;
            Scholarship s = scholarshipDAO.findById(app.getScholarshipId());
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new ApplicationDialog(parent, s, app).setVisible(true);
            refresh();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}