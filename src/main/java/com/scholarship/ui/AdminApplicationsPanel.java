package com.scholarship.ui;

import com.scholarship.dao.AdminApplicationDAO;
import com.scholarship.model.Application;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Admin tab: all submitted applications with search, status filter and statistics. */
public class AdminApplicationsPanel extends JPanel {
    private final AdminApplicationDAO dao = new AdminApplicationDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Student", "Scholarship", "Status", "Submitted", "Last updated"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All", "SUBMITTED", "UNDER_REVIEW", "DOCUMENTS_REQUESTED", "APPROVED", "REJECTED"});
    private final JLabel statsLabel = new JLabel(" ");

    public AdminApplicationsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search (student or scholarship):"));
        filters.add(searchField);
        filters.add(new JLabel("Status:"));
        filters.add(statusFilter);
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(statsLabel);
        top.add(filters);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        statusFilter.addActionListener(e -> applyFilter());

        JButton reviewBtn = new JButton("Review Selected Application");
        JButton refreshBtn = new JButton("Refresh");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(reviewBtn);
        bottom.add(refreshBtn);
        reviewBtn.addActionListener(e -> reviewSelected());
        refreshBtn.addActionListener(e -> refresh());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    public void refresh() {
        try {
            List<Application> data = dao.findAll();
            model.setRowCount(0);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String s : new String[]{"SUBMITTED", "UNDER_REVIEW", "DOCUMENTS_REQUESTED", "APPROVED", "REJECTED"}) {
                counts.put(s, 0);
            }
            for (Application a : data) {
                model.addRow(new Object[]{a.getId(), a.getStudentName(), a.getScholarshipName(),
                        a.getStatus(), a.getSubmittedAt(), a.getUpdatedAt()});
                counts.merge(a.getStatus(), 1, Integer::sum);
            }
            StringBuilder sb = new StringBuilder("Total: " + data.size());
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                sb.append("   |   ").append(e.getKey()).append(": ").append(e.getValue());
            }
            statsLabel.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String text = searchField.getText().trim();
        if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2));
        String status = (String) statusFilter.getSelectedItem();
        if (!"All".equals(status)) filters.add(RowFilter.regexFilter("^" + status + "$", 3));
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void reviewSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an application first.");
            return;
        }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        try {
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new ReviewDialog(parent, id).setVisible(true);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}