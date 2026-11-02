package com.scholarship.ui;

import com.scholarship.dao.ScholarshipDAO;
import com.scholarship.model.Scholarship;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Admin tab: shows all scholarships in a JTable with Add / Edit / Publish / Deactivate. */
public class ScholarshipPanel extends JPanel {
    private final ScholarshipDAO dao = new ScholarshipDAO();
    private List<Scholarship> data = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Sponsor", "Amount", "Opens", "Closes", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{"All", "DRAFT", "PUBLISHED", "INACTIVE"});

    public ScholarshipPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // top bar: search + filter
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(new JLabel("Status:"));
        top.add(statusFilter);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        statusFilter.addActionListener(e -> applyFilter());

        // bottom bar: buttons
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton publishBtn = new JButton("Publish");
        JButton deactivateBtn = new JButton("Deactivate");
        JButton viewBtn = new JButton("View Details");
        JButton refreshBtn = new JButton("Refresh");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : new JButton[]{addBtn, editBtn, viewBtn, publishBtn, deactivateBtn, refreshBtn}) bottom.add(b);

        addBtn.addActionListener(e -> openDialog(null));
        editBtn.addActionListener(e -> { Scholarship s = selected(); if (s != null) openDialog(s); });
        viewBtn.addActionListener(e -> showDetails());
        publishBtn.addActionListener(e -> publish());
        deactivateBtn.addActionListener(e -> deactivate());
        refreshBtn.addActionListener(e -> loadData());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        loadData();
    }

    private Frame parentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void loadData() {
        try {
            data = dao.findAll();
            model.setRowCount(0);
            for (Scholarship s : data) {
                model.addRow(new Object[]{s.getId(), s.getName(), s.getSponsor(),
                        String.format("%.2f", s.getAmount()), s.getOpenDate(), s.getCloseDate(), s.getStatus()});
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2)); // name, sponsor
        String status = (String) statusFilter.getSelectedItem();
        if (!"All".equals(status)) filters.add(RowFilter.regexFilter("^" + status + "$", 6));
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    /** Finds the Scholarship object for the selected table row (works even when the table is sorted/filtered). */
    private Scholarship selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a scholarship in the table first.");
            return null;
        }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        for (Scholarship s : data) if (s.getId() == id) return s;
        return null;
    }

    private void openDialog(Scholarship s) {
        ScholarshipDialog d = new ScholarshipDialog(parentFrame(), s);
        d.setVisible(true);
        if (d.isSaved()) loadData();
    }

    private void publish() {
        Scholarship s = selected();
        if (s == null) return;
        if ("PUBLISHED".equals(s.getStatus())) { JOptionPane.showMessageDialog(this, "Already published."); return; }
        if (s.getCloseDate().isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "The closing date has already passed. Edit the dates first.");
            return;
        }
        changeStatus(s, "PUBLISHED", "Publish \"" + s.getName() + "\"? Students will be able to see it.");
    }

    private void deactivate() {
        Scholarship s = selected();
        if (s == null) return;
        if ("INACTIVE".equals(s.getStatus())) { JOptionPane.showMessageDialog(this, "Already inactive."); return; }
        changeStatus(s, "INACTIVE", "Deactivate \"" + s.getName() + "\"? Students will no longer see it.\n"
                + "(Existing applications and decisions are kept.)");
    }

    private void changeStatus(Scholarship s, String newStatus, String question) {
        int ok = JOptionPane.showConfirmDialog(this, question, "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.setStatus(s.getId(), newStatus);
            loadData();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void showDetails() {
        Scholarship s = selected();
        if (s == null) return;
        String text = "Name: " + s.getName()
                + "\nSponsor: " + s.getSponsor()
                + "\nAmount: " + String.format("%.2f", s.getAmount())
                + "\nApplication period: " + s.getOpenDate() + " to " + s.getCloseDate()
                + "\nStatus: " + s.getStatus()
                + "\n\nDescription:\n" + s.getDescription()
                + "\n\nEligibility:\n" + s.getEligibility()
                + "\nMinimum %: " + (s.getMinPercentage() == null ? "No rule" : s.getMinPercentage())
                + "\nIncome limit: " + (s.getIncomeLimit() == null ? "No rule" : s.getIncomeLimit())
                + "\nCourse: " + s.getEligibleCourse()
                + "\nEducation level: " + s.getEducationLevel()
                + "\n\nRequired documents: " + (s.getRequiredDocs().isEmpty() ? "None" : String.join(", ", s.getRequiredDocs()));
        JTextArea area = new JTextArea(text, 18, 40);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Scholarship Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}