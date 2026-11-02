package com.scholarship.dao;

import com.scholarship.model.Application;
import com.scholarship.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for applications. Every student query also checks student_id, so students only see their own data. */
public class ApplicationDAO {

    private static final String SELECT_BASE =
            "SELECT a.*, s.name AS scholarship_name, s.close_date FROM applications a "
                    + "JOIN scholarships s ON a.scholarship_id = s.scholarship_id ";

    public List<Application> findByStudent(int studentId) throws SQLException {
        List<Application> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE a.student_id=? ORDER BY a.updated_at DESC")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Application findForStudent(int studentId, int scholarshipId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE a.student_id=? AND a.scholarship_id=?")) {
            ps.setInt(1, studentId);
            ps.setInt(2, scholarshipId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Application findByIdForStudent(int applicationId, int studentId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE a.application_id=? AND a.student_id=?")) {
            ps.setInt(1, applicationId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int createDraft(int studentId, int scholarshipId, String statement) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO applications(student_id,scholarship_id,status,statement) VALUES(?,?,'DRAFT',?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, scholarshipId);
                    ps.setString(3, statement);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                addHistory(c, id, null, "DRAFT", "Draft created", studentId);
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void saveStatement(int applicationId, int studentId, String statement) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE applications SET statement=? WHERE application_id=? AND student_id=? "
                             + "AND status IN ('DRAFT','DOCUMENTS_REQUESTED')")) {
            ps.setString(1, statement);
            ps.setInt(2, applicationId);
            ps.setInt(3, studentId);
            ps.executeUpdate();
        }
    }

    /** Final submit (also used when the student answers a "documents requested" message). */
    public void submit(int applicationId, int studentId, String statement) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT status FROM applications WHERE application_id=? AND student_id=? FOR UPDATE")) {
                    ps.setInt(1, applicationId);
                    ps.setInt(2, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Application not found.");
                        oldStatus = rs.getString(1);
                    }
                }
                if (!"DRAFT".equals(oldStatus) && !"DOCUMENTS_REQUESTED".equals(oldStatus)) {
                    throw new SQLException("This application can no longer be changed.");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE applications SET statement=?, status='SUBMITTED', "
                                + "submitted_at=COALESCE(submitted_at, NOW()) WHERE application_id=?")) {
                    ps.setString(1, statement);
                    ps.setInt(2, applicationId);
                    ps.executeUpdate();
                }
                String note = "DRAFT".equals(oldStatus) ? "Application submitted by student"
                        : "Student responded to the document request";
                addHistory(c, applicationId, oldStatus, "SUBMITTED", note, studentId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void addDocument(int applicationId, String documentName, String filePath) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO application_documents(application_id,document_name,file_path) VALUES(?,?,?)")) {
            ps.setInt(1, applicationId);
            ps.setString(2, documentName);
            ps.setString(3, filePath);
            ps.executeUpdate();
        }
    }

    /** Each row: {document name, file path, uploaded time}. Oldest first. */
    public List<String[]> getDocuments(int applicationId) throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT document_name,file_path,uploaded_at FROM application_documents "
                             + "WHERE application_id=? ORDER BY document_id")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{rs.getString(1), rs.getString(2), String.valueOf(rs.getTimestamp(3))});
                }
            }
        }
        return list;
    }

    /** Each row: {date, old status, new status, note, changed by}. */
    public List<String[]> getHistory(int applicationId) throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT h.changed_at,h.old_status,h.new_status,h.note,u.full_name FROM status_history h "
                             + "JOIN users u ON h.changed_by=u.user_id WHERE h.application_id=? ORDER BY h.history_id")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String old = rs.getString(2);
                    String note = rs.getString(4);
                    list.add(new String[]{
                            rs.getTimestamp(1).toString().substring(0, 16),
                            old == null ? "-" : old,
                            rs.getString(3),
                            note == null ? "" : note,
                            rs.getString(5)});
                }
            }
        }
        return list;
    }

    private void addHistory(Connection c, int applicationId, String oldStatus, String newStatus,
                            String note, int changedBy) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO status_history(application_id,old_status,new_status,note,changed_by) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, applicationId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            ps.setString(4, note);
            ps.setInt(5, changedBy);
            ps.executeUpdate();
        }
    }

    private Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setId(rs.getInt("application_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setScholarshipId(rs.getInt("scholarship_id"));
        a.setScholarshipName(rs.getString("scholarship_name"));
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