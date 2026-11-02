package com.scholarship.ui;

import com.scholarship.dao.AdminApplicationDAO;
import com.scholarship.dao.ApplicationDAO;
import com.scholarship.dao.ProfileDAO;
import com.scholarship.dao.ScholarshipDAO;
import com.scholarship.model.Application;
import com.scholarship.model.Scholarship;
import com.scholarship.model.StudentProfile;
import com.scholarship.util.EligibilityChecker;
import com.scholarship.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Admin window to review one application and change its status. */
public class ReviewDialog extends JDialog {
    private final AdminApplicationDAO adminDAO = new AdminApplicationDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final ScholarshipDAO scholarshipDAO = new ScholarshipDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final int adminId = Session.getCurrentUser().getId();

    private Application app;
    private final Scholarship scholarship;
    private List<String[]> docs = new ArrayList<>();

    private final JLabel statusLabel = new JLabel();
    private final JTextArea summaryArea = new JTextArea();
    private final JLabel missingLabel = new JLabel();
    private final DefaultTableModel docModel = new DefaultTableModel(
            new String[]{"Document", "File", "Uploaded"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable docTable = new JTable(docModel);
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Date", "From", "To", "Note", "By"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JButton reviewBtn = new JButton("Mark Under Review");
    private final JButton requestBtn = new JButton("Request Documents");
    private final JButton approveBtn = new JButton("Approve");
    private final JButton rejectBtn = new JButton("Reject");

    public ReviewDialog(Frame parent, int applicationId) throws SQLException {
        super(parent, "Review Application", true);
        app = adminDAO.findById(applicationId);
        if (app == null) throw new SQLException("Application not found.");
        scholarship = scholarshipDAO.findById(app.getScholarshipId());

        JLabel title = new JLabel("Application #" + app.getId() + "  |  " + app.getStudentName()
                + "  |  " + app.getScholarshipName());
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 5, 12));
        top.add(title);
        top.add(statusLabel);

        // Tab 1: summary
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);

        // Tab 2: documents
        JButton openBtn = new JButton("Open Selected File");
        openBtn.addActionListener(e -> openFile());
        JPanel docsTab = new JPanel(new BorderLayout(5, 5));
        docsTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        docsTab.add(missingLabel, BorderLayout.NORTH);
        docsTab.add(new JScrollPane(docTable), BorderLayout.CENTER);
        docsTab.add(openBtn, BorderLayout.SOUTH);

        // Tab 3: history
        JPanel historyTab = new JPanel(new BorderLayout());
        historyTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        historyTab.add(new JScrollPane(new JTable(historyModel)), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Applicant & Statement", new JScrollPane(summaryArea));
        tabs.addTab("Documents", docsTab);
        tabs.addTab("Status History", historyTab);

        JButton closeBtn = new JButton("Close");
        JPanel buttons = new JPanel();
        buttons.add(reviewBtn);
        buttons.add(requestBtn);
        buttons.add(approveBtn);
        buttons.add(rejectBtn);
        buttons.add(closeBtn);

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reviewBtn.addActionListener(e -> markUnderReview());
        requestBtn.addActionListener(e -> requestDocuments());
        approveBtn.addActionListener(e -> decide("APPROVED", "Approve"));
        rejectBtn.addActionListener(e -> decide("REJECTED", "Reject"));
        closeBtn.addActionListener(e -> dispose());

        refresh();
        setSize(820, 600);
        setLocationRelativeTo(parent);
    }

    // ---------- screen update ----------

    private void refresh() throws SQLException {
        app = adminDAO.findById(app.getId());
        String st = app.getStatus();
        statusLabel.setText("Status: " + st);

        summaryArea.setText(buildSummary());
        summaryArea.setCaretPosition(0);

        docModel.setRowCount(0);
        docs = appDAO.getDocuments(app.getId());
        List<String> uploadedNames = new ArrayList<>();
        for (String[] d : docs) {
            docModel.addRow(new Object[]{d[0], new File(d[1]).getName(), d[2].substring(0, 16)});
            uploadedNames.add(d[0]);
        }
        List<String> missing = missingDocs(uploadedNames);
        missingLabel.setText(missing.isEmpty() ? "All required documents have been uploaded."
                : "Missing required documents: " + String.join(", ", missing));

        historyModel.setRowCount(0);
        for (String[] h : appDAO.getHistory(app.getId())) historyModel.addRow(h);

        reviewBtn.setEnabled(st.equals("SUBMITTED"));
        requestBtn.setEnabled(st.equals("SUBMITTED") || st.equals("UNDER_REVIEW"));
        approveBtn.setEnabled(st.equals("UNDER_REVIEW"));
        rejectBtn.setEnabled(st.equals("UNDER_REVIEW"));
    }

    private List<String> missingDocs(List<String> uploadedNames) {
        List<String> missing = new ArrayList<>();
        for (String req : scholarship.getRequiredDocs()) {
            if (!uploadedNames.contains(req)) missing.add(req);
        }
        return missing;
    }

    private String buildSummary() throws SQLException {
        StudentProfile p = profileDAO.get(app.getStudentId());
        List<String> problems = EligibilityChecker.check(scholarship, p);
        StringBuilder sb = new StringBuilder();
        sb.append("STUDENT\n");
        sb.append("Name: ").append(app.getStudentName()).append("\n");
        sb.append("Email: ").append(app.getStudentEmail()).append("\n");
        sb.append("Phone: ").append(v(p.getPhone())).append("\n");
        sb.append("Date of birth: ").append(v(p.getDateOfBirth())).append("\n");
        sb.append("Course: ").append(v(p.getCourse())).append("   Level: ").append(v(p.getEducationLevel())).append("\n");
        sb.append("Percentage: ").append(v(p.getPercentage())).append("\n");
        sb.append("Family income: ").append(v(p.getFamilyIncome())).append("\n");
        sb.append("Address: ").append(v(p.getAddress())).append("\n");

        sb.append("\nSCHOLARSHIP RULES\n");
        sb.append("Minimum %: ").append(scholarship.getMinPercentage() == null ? "No rule" : scholarship.getMinPercentage()).append("\n");
        sb.append("Income limit: ").append(scholarship.getIncomeLimit() == null ? "No rule" : scholarship.getIncomeLimit()).append("\n");
        sb.append("Course: ").append(v(scholarship.getEligibleCourse()))
                .append("   Level: ").append(v(scholarship.getEducationLevel())).append("\n");

        sb.append("\nELIGIBILITY CHECK (based on the student's current profile)\n");
        sb.append(problems.isEmpty() ? "Meets all rules.\n" : "- " + String.join("\n- ", problems) + "\n");

        sb.append("\nSTATEMENT\n").append(v(app.getStatement())).append("\n");

        if (app.getDecisionReason() != null && !app.getDecisionReason().isBlank()) {
            sb.append("\nFINAL DECISION (").append(app.getStatus()).append(")\n").append(app.getDecisionReason()).append("\n");
        }
        return sb.toString();
    }

    private String v(Object o) {
        return o == null || o.toString().isBlank() ? "-" : o.toString();
    }

    // ---------- actions ----------

    private void markUnderReview() {
        int ok = JOptionPane.showConfirmDialog(this, "Mark this application as under review?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) doChange("UNDER_REVIEW", "Marked under review by administrator");
    }

    private void requestDocuments() {
        List<String> uploadedNames = new ArrayList<>();
        for (String[] d : docs) uploadedNames.add(d[0]);
        List<String> missing = missingDocs(uploadedNames);
        String prefill = missing.isEmpty() ? "" : "Please upload: " + String.join(", ", missing);

        String text = askText("Request documents", "Tell the student what is missing or unclear:", prefill);
        if (text != null) doChange("DOCUMENTS_REQUESTED", text);
    }

    private void decide(String newStatus, String verb) {
        String text = askText(verb + " application", "Write the reason for this decision (the student will see it):", "");
        if (text == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                verb + " this application? This decision is final and cannot be changed.",
                "Confirm decision", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) doChange(newStatus, text);
    }

    private void doChange(String newStatus, String note) {
        try {
            adminDAO.changeStatus(app.getId(), newStatus, note, adminId);
            refresh();
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private String askText(String title, String prompt, String prefill) {
        JTextArea area = new JTextArea(prefill, 5, 35);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(new JLabel(prompt), BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        while (true) {
            int ok = JOptionPane.showConfirmDialog(this, p, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return null;
            String t = area.getText().trim();
            if (t.length() >= 5) return t;
            JOptionPane.showMessageDialog(this, "Please write at least 5 characters.");
        }
    }

    private void openFile() {
        int row = docTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a document in the table first.");
            return;
        }
        File f = new File(docs.get(row)[1]);
        try {
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, "File not found: " + f.getPath());
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}