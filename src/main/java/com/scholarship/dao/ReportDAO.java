package com.scholarship.dao;

import com.scholarship.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SQL for the reports. Drafts are never counted because the admin cannot see drafts. */
public class ReportDAO {

    public static final String[] SUMMARY_HEADERS = {"ID", "Scholarship", "Status", "Amount", "Total applications",
            "Submitted", "Under review", "Documents requested", "Approved", "Rejected", "Approved total (demo)"};

    public static final String[] DETAIL_HEADERS = {"App ID", "Student", "Email", "Scholarship", "Status",
            "Submitted", "Last updated", "Decision reason"};

    /** Small numbers shown at the top of the Reports tab. */
    public Map<String, String> overview() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection()) {
            m.put("Registered students", String.valueOf(scalar(c, "SELECT COUNT(*) FROM users WHERE role='STUDENT'")));
            m.put("Published scholarships", String.valueOf(scalar(c, "SELECT COUNT(*) FROM scholarships WHERE status='PUBLISHED'")));
            m.put("Open for applications now", String.valueOf(scalar(c,
                    "SELECT COUNT(*) FROM scholarships WHERE status='PUBLISHED' AND CURDATE() BETWEEN open_date AND close_date")));
            m.put("Applications submitted", String.valueOf(scalar(c, "SELECT COUNT(*) FROM applications WHERE status<>'DRAFT'")));
            m.put("Pending (need action)", String.valueOf(scalar(c,
                    "SELECT COUNT(*) FROM applications WHERE status IN ('SUBMITTED','UNDER_REVIEW','DOCUMENTS_REQUESTED')")));
            int approved = scalar(c, "SELECT COUNT(*) FROM applications WHERE status='APPROVED'");
            int rejected = scalar(c, "SELECT COUNT(*) FROM applications WHERE status='REJECTED'");
            m.put("Approved", String.valueOf(approved));
            m.put("Rejected", String.valueOf(rejected));
            int decided = approved + rejected;
            m.put("Approval rate", decided == 0 ? "-" : String.format("%.1f%%", approved * 100.0 / decided));
        }
        return m;
    }

    /** One row per scholarship with a count for each status. */
    public List<String[]> scholarshipSummary() throws SQLException {
        String sql = "SELECT s.scholarship_id, s.name, s.status, s.amount, "
                + "COUNT(a.application_id) AS total, "
                + "COALESCE(SUM(a.status='SUBMITTED'),0) AS submitted, "
                + "COALESCE(SUM(a.status='UNDER_REVIEW'),0) AS under_review, "
                + "COALESCE(SUM(a.status='DOCUMENTS_REQUESTED'),0) AS docs, "
                + "COALESCE(SUM(a.status='APPROVED'),0) AS approved, "
                + "COALESCE(SUM(a.status='REJECTED'),0) AS rejected "
                + "FROM scholarships s "
                + "LEFT JOIN applications a ON a.scholarship_id=s.scholarship_id AND a.status<>'DRAFT' "
                + "GROUP BY s.scholarship_id, s.name, s.status, s.amount "
                + "ORDER BY s.scholarship_id DESC";
        List<String[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double amount = rs.getDouble("amount");
                int approved = rs.getInt("approved");
                rows.add(new String[]{
                        String.valueOf(rs.getInt("scholarship_id")),
                        rs.getString("name"),
                        rs.getString("status"),
                        String.format("%.2f", amount),
                        String.valueOf(rs.getInt("total")),
                        String.valueOf(rs.getInt("submitted")),
                        String.valueOf(rs.getInt("under_review")),
                        String.valueOf(rs.getInt("docs")),
                        String.valueOf(approved),
                        String.valueOf(rs.getInt("rejected")),
                        String.format("%.2f", approved * amount)});
            }
        }
        return rows;
    }

    /** Every submitted application. Pass null as statusFilter for all statuses. */
    public List<String[]> applicationDetails(String statusFilter) throws SQLException {
        String sql = "SELECT a.application_id, u.full_name, u.email, s.name, a.status, "
                + "a.submitted_at, a.updated_at, a.decision_reason "
                + "FROM applications a "
                + "JOIN users u ON a.student_id=u.user_id "
                + "JOIN scholarships s ON a.scholarship_id=s.scholarship_id "
                + "WHERE a.status<>'DRAFT' "
                + (statusFilter == null ? "" : "AND a.status=? ")
                + "ORDER BY a.application_id DESC";
        List<String[]> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (statusFilter != null) ps.setString(1, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            String.valueOf(rs.getInt(1)),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            time(rs.getTimestamp(6)),
                            time(rs.getTimestamp(7)),
                            rs.getString(8) == null ? "" : rs.getString(8)});
                }
            }
        }
        return rows;
    }

    private int scalar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private String time(Timestamp t) {
        return t == null ? "" : t.toString().substring(0, 16);
    }
}