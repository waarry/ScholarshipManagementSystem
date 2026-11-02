package com.scholarship.dao;

import com.scholarship.model.Application;
import com.scholarship.util.DBConnection;
import com.scholarship.util.StatusRules;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** SQL used by the administrator to review applications. Drafts are never shown to the admin. */
public class AdminApplicationDAO {

    private static final String SELECT_BASE =
            "SELECT a.*, s.name AS scholarship_name, s.close_date, "
                    + "u.full_name AS student_name, u.email AS student_email "
                    + "FROM applications a "
                    + "JOIN scholarships s ON a.scholarship_id = s.scholarship_id "
                    + "JOIN users u ON a.student_id = u.user_id "
                    + "WHERE a.status <> 'DRAFT' ";

    public List<Application> findAll() throws SQLException {
        List<Application> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY a.updated_at DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Application findById(int applicationId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "AND a.application_id=?")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Changes the status, saves the reason, and writes a history row, all in one transaction.
     * For APPROVED / REJECTED the reason is also stored as the final decision reason.
     */
    public void changeStatus(int applicationId, String newStatus, String note, int adminId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT status FROM applications WHERE application_id=? FOR UPDATE")) {
                    ps.setInt(1, applicationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Application not found.");
                        oldStatus = rs.getString(1);
                    }
                }
                if (!StatusRules.isAllowed(oldStatus, newStatus)) {
                    throw new SQLException("Cannot change status from " + oldStatus + " to " + newStatus + ".");
                }

                boolean isDecision = newStatus.equals("APPROVED") || newStatus.equals("REJECTED");
                String sql = isDecision
                        ? "UPDATE applications SET status=?, decision_reason=? WHERE application_id=?"
                        : "UPDATE applications SET status=? WHERE application_id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    if (isDecision) {
                        ps.setString(2, note);
                        ps.setInt(3, applicationId);
                    } else {
                        ps.setInt(2, applicationId);
                    }
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO status_history(application_id,old_status,new_status,note,changed_by) "
                                + "VALUES(?,?,?,?,?)")) {
                    ps.setInt(1, applicationId);
                    ps.setString(2, oldStatus);
                    ps.setString(3, newStatus);
                    ps.setString(4, note);
                    ps.setInt(5, adminId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setId(rs.getInt("application_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setScholarshipId(rs.getInt("scholarship_id"));
        a.setScholarshipName(rs.getString("scholarship_name"));
        a.setStudentName(rs.getString("student_name"));
        a.setStudentEmail(rs.getString("student_email"));
        a.setStatus(rs.getString("status"));
        a.setStatement(rs.getString("statement"));
        a.setDecisionReason(rs.getString("decision_reason"));
        Timestamp sub = rs.getTimestamp("submitted_at");
        a.setSubmittedAt(sub == null ? "-" : sub.toString().substring(0, 16));
        Timestamp upd = rs.getTimestamp("updated_at");
        a.setUpdatedAt(upd == null ? "-" : upd.toString().substring(0, 16));
        a.setCloseDate(rs.getDate("close_date").toLocalDate());
        return a;
    }
}