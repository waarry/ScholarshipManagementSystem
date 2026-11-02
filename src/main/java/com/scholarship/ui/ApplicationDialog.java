package com.scholarship.ui;

import com.scholarship.dao.ApplicationDAO;
import com.scholarship.dao.ProfileDAO;
import com.scholarship.model.Application;
import com.scholarship.model.Scholarship;
import com.scholarship.util.EligibilityChecker;
import com.scholarship.util.Session;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Apply form: statement, document upload, save draft, submit, plus status history and feedback. */
public class ApplicationDialog extends JDialog {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;   // 5 MB

    private final Scholarship scholarship;
    private Application app;                       // null until the draft is first saved
    private final int studentId = Session.getCurrentUser().getId();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();

    private final JTextArea statementArea = new JTextArea(8, 45);
    private final JLabel statusLabel = new JLabel();
    private final JTextArea feedbackArea = new JTextArea(4, 45);
    private final DefaultTableModel docModel = new DefaultTableModel(
            new String[]{"Required document", "Status", "Uploaded file"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Date", "From", "To", "Note", "By"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable docTable = new JTable(docModel);
    private final JButton saveBtn = new JButton("Save Draft");
    private final JButton submitBtn = new JButton("Submit Application");
    private final JButton uploadBtn = new JButton("Upload File for Selected Document");

    public ApplicationDialog(Frame parent, Scholarship scholarship, Application existing) {
        super(parent, "Application - " + scholarship.getName(), true);
        this.scholarship = scholarship;
        this.app = existing;

        JLabel title = new JLabel(scholarship.getName() + "   |   Amount: "
                + String.format("%.2f", scholarship.getAmount())
                + "   |   Deadline: " + scholarship.getCloseDate());
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 5, 12));
        top.add(title);
        top.add(statusLabel);

        // Tab 1: form
        statementArea.setLineWrap(true);
        statementArea.setWrapStyleWord(true);
        if (existing != null && existing.getStatement() != null) statementArea.setText(existing.getStatement());
        JPanel formTab = new JPanel(new BorderLayout(5, 5));
        formTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        formTab.add(new JLabel("Statement: why do you need this scholarship? (minimum 20 characters)"), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(5, 5));
        mid.add(new JScrollPane(statementArea), BorderLayout.CENTER);
        JPanel docsPanel = new JPanel(new BorderLayout(5, 5));
        docsPanel.add(new JScrollPane(docTable) {{ setPreferredSize(new Dimension(300, 110)); }}, BorderLayout.CENTER);
        docsPanel.add(uploadBtn, BorderLayout.SOUTH);
        mid.add(docsPanel, BorderLayout.SOUTH);
        formTab.add(mid, BorderLayout.CENTER);

        // Tab 2: feedback + history
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        JPanel historyTab = new JPanel(new BorderLayout(5, 5));
        historyTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel fbPanel = new JPanel(new BorderLayout());
        fbPanel.add(new JLabel("Administrator feedback:"), BorderLayout.NORTH);
        fbPanel.add(new JScrollPane(feedbackArea), BorderLayout.CENTER);
        historyTab.add(fbPanel, BorderLayout.NORTH);
        historyTab.add(new JScrollPane(new JTable(historyModel)), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Application Form", formTab);
        tabs.addTab("Status & Feedback", historyTab);

        JButton closeBtn = new JButton("Close");
        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(submitBtn);
        buttons.add(closeBtn);

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> saveDraft());
        submitBtn.addActionListener(e -> submit());
        uploadBtn.addActionListener(e -> upload());
        closeBtn.addActionListener(e -> dispose());

        try {
            refreshAll();
        } catch (Exception ex) {
            error(ex);
        }
        pack();
        setLocationRelativeTo(parent);
    }

    /** Re-reads documents and history from the database and updates every part of the screen. */
    private void refreshAll() throws SQLException {
        boolean editable = app == null || app.isEditable();
        statementArea.setEditable(editable);
        saveBtn.setEnabled(editable);
        submitBtn.setEnabled(editable);
        uploadBtn.setEnabled(editable && !scholarship.getRequiredDocs().isEmpty());
        statusLabel.setText("Status: " + (app == null ? "NOT STARTED" : app.getStatus()));

        docModel.setRowCount(0);
        historyModel.setRowCount(0);
        Map<String, String[]> latestDoc = new HashMap<>();
        String lastRequestNote = null;
        if (app != null) {
            for (String[] d : appDAO.getDocuments(app.getId())) latestDoc.put(d[0], d);   // later rows win
            for (String[] h : appDAO.getHistory(app.getId())) {
                historyModel.addRow(h);
                if ("DOCUMENTS_REQUESTED".equals(h[2])) lastRequestNote = h[3];
            }
        }
        for (String req : scholarship.getRequiredDocs()) {
            String[] d = latestDoc.get(req);
            docModel.addRow(new Object[]{req, d == null ? "MISSING" : "UPLOADED",
                    d == null ? "" : new File(d[1]).getName()});
        }

        String feedback = "No feedback yet.";
        if (app != null) {
            String reason = app.getDecisionReason();
            if (reason != null && !reason.isBlank()) {
                feedback = "Decision (" + app.getStatus() + "): " + reason;
            } else if ("DOCUMENTS_REQUESTED".equals(app.getStatus()) && lastRequestNote != null) {
                feedback = "Administrator requests: " + lastRequestNote
                        + "\n\nUpload the missing documents in the Application Form tab, then submit again.";
            }
        }
        feedbackArea.setText(feedback);
    }

    private boolean isOpenNow() {
        LocalDate today = LocalDate.now();
        return "PUBLISHED".equals(scholarship.getStatus())
                && !today.isBefore(scholarship.getOpenDate())
                && !today.isAfter(scholarship.getCloseDate());
    }

    /** Creates the draft row the first time it is needed. Returns false if the student may not start. */
    private boolean ensureDraft() throws SQLException {
        if (app != null) return true;
        if (!isOpenNow()) {
            warn("This scholarship is not open for applications today.");
            return false;
        }
        int id = appDAO.createDraft(studentId, scholarship.getId(), statementArea.getText().trim());
        app = appDAO.findByIdForStudent(id, studentId);
        refreshAll();
        return true;
    }

    private void saveDraft() {
        try {
            if (app == null) {
                if (!ensureDraft()) return;
            } else if (app.isEditable()) {
                appDAO.saveStatement(app.getId(), studentId, statementArea.getText().trim());
            }
            JOptionPane.showMessageDialog(this, "Saved.");
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void upload() {
        int row = docTable.getSelectedRow();
        if (row < 0) { warn("Select a required document in the table first."); return; }
        String docName = (String) docModel.getValueAt(row, 0);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF, JPG or PNG files", "pdf", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (file.length() > MAX_FILE_SIZE) { warn("File is too large. Maximum size is 5 MB."); return; }

        try {
            if (!ensureDraft()) return;
            Path dir = Paths.get("uploads", "app_" + app.getId());
            Files.createDirectories(dir);
            String name = file.getName();
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            String safe = docName.replaceAll("[^A-Za-z0-9]", "_");
            Path dest = dir.resolve(safe + "_" + System.currentTimeMillis() + "." + ext);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            appDAO.addDocument(app.getId(), docName, dest.toString());
            refreshAll();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private List<String> missingDocs() throws SQLException {
        List<String> uploaded = new ArrayList<>();
        if (app != null) {
            for (String[] d : appDAO.getDocuments(app.getId())) uploaded.add(d[0]);
        }
        List<String> missing = new ArrayList<>();
        for (String req : scholarship.getRequiredDocs()) {
            if (!uploaded.contains(req)) missing.add(req);
        }
        return missing;
    }

    private void submit() {
        try {
            if (app != null && !app.isEditable()) return;

            String statement = statementArea.getText().trim();
            if (statement.length() < 20) { warn("Please write at least 20 characters in the statement."); return; }

            List<String> problems = EligibilityChecker.check(scholarship, profileDAO.get(studentId));
            if (!problems.isEmpty()) {
                warn("You are not eligible:\n- " + String.join("\n- ", problems));
                return;
            }

            boolean firstSubmit = app == null || "DRAFT".equals(app.getStatus());
            if (firstSubmit && !isOpenNow()) {
                warn("This scholarship is not open for applications today.");
                return;
            }

            List<String> missing = missingDocs();
            if (!missing.isEmpty()) { warn("Please upload these documents first:\n- " + String.join("\n- ", missing)); return; }

            int ok = JOptionPane.showConfirmDialog(this,
                    "Submit your application? You cannot edit it after submitting.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;

            if (!ensureDraft()) return;
            appDAO.submit(app.getId(), studentId, statement);
            JOptionPane.showMessageDialog(this, "Application submitted successfully!");
            dispose();
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}