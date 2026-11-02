package com.scholarship.dao;

import com.scholarship.model.Scholarship;
import com.scholarship.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All SQL for scholarships and their required documents. */
public class ScholarshipDAO {

    private static final String INSERT_SQL =
            "INSERT INTO scholarships(name,description,sponsor,eligibility,min_percentage,income_limit,"
                    + "eligible_course,education_level,amount,open_date,close_date,status,created_by) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_SQL =
            "UPDATE scholarships SET name=?,description=?,sponsor=?,eligibility=?,min_percentage=?,income_limit=?,"
                    + "eligible_course=?,education_level=?,amount=?,open_date=?,close_date=?,status=? WHERE scholarship_id=?";

    public List<Scholarship> findAll() throws SQLException {
        List<Scholarship> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM scholarships ORDER BY scholarship_id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        for (Scholarship s : list) s.setRequiredDocs(getDocs(s.getId()));
        return list;
    }

    public void insert(Scholarship s, int adminId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    fill(ps, s);
                    ps.setInt(13, adminId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                saveDocs(c, newId, s.getRequiredDocs());
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void update(Scholarship s) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(UPDATE_SQL)) {
                    fill(ps, s);
                    ps.setInt(13, s.getId());
                    ps.executeUpdate();
                }
                saveDocs(c, s.getId(), s.getRequiredDocs());
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Used for Publish and Deactivate. We never delete scholarships, so history stays safe. */
    public void setStatus(int id, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE scholarships SET status=? WHERE scholarship_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<String> getDocs(int scholarshipId) throws SQLException {
        List<String> docs = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT document_name FROM required_documents WHERE scholarship_id=? ORDER BY doc_req_id")) {
            ps.setInt(1, scholarshipId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) docs.add(rs.getString(1));
            }
        }
        return docs;
    }
    /** Scholarships a student may see: published and open today. */
    public List<Scholarship> findAvailable() throws SQLException {
        List<Scholarship> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM scholarships WHERE status='PUBLISHED' "
                     + "AND CURDATE() BETWEEN open_date AND close_date ORDER BY close_date")) {
            while (rs.next()) list.add(map(rs));
        }
        for (Scholarship s : list) s.setRequiredDocs(getDocs(s.getId()));
        return list;
    }

    public Scholarship findById(int id) throws SQLException {
        Scholarship s = null;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM scholarships WHERE scholarship_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) s = map(rs);
            }
        }
        if (s != null) s.setRequiredDocs(getDocs(id));
        return s;
    }

    // ---------- helpers ----------

    private void fill(PreparedStatement ps, Scholarship s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getSponsor());
        ps.setString(4, s.getEligibility());
        setNullableDouble(ps, 5, s.getMinPercentage());
        setNullableDouble(ps, 6, s.getIncomeLimit());
        ps.setString(7, s.getEligibleCourse());
        ps.setString(8, s.getEducationLevel());
        ps.setDouble(9, s.getAmount());
        ps.setDate(10, Date.valueOf(s.getOpenDate()));
        ps.setDate(11, Date.valueOf(s.getCloseDate()));
        ps.setString(12, s.getStatus());
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DECIMAL);
        else ps.setDouble(index, value);
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private void saveDocs(Connection c, int scholarshipId, List<String> docs) throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM required_documents WHERE scholarship_id=?")) {
            del.setInt(1, scholarshipId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO required_documents(scholarship_id,document_name) VALUES(?,?)")) {
            for (String d : docs) {
                ins.setInt(1, scholarshipId);
                ins.setString(2, d);
                ins.executeUpdate();
            }
        }
    }

    private Scholarship map(ResultSet rs) throws SQLException {
        Scholarship s = new Scholarship();
        s.setId(rs.getInt("scholarship_id"));
        s.setName(rs.getString("name"));
        s.setDescription(rs.getString("description"));
        s.setSponsor(rs.getString("sponsor"));
        s.setEligibility(rs.getString("eligibility"));
        s.setMinPercentage(getNullableDouble(rs, "min_percentage"));
        s.setIncomeLimit(getNullableDouble(rs, "income_limit"));
        s.setEligibleCourse(rs.getString("eligible_course"));
        s.setEducationLevel(rs.getString("education_level"));
        s.setAmount(rs.getDouble("amount"));
        s.setOpenDate(rs.getDate("open_date").toLocalDate());
        s.setCloseDate(rs.getDate("close_date").toLocalDate());
        s.setStatus(rs.getString("status"));
        return s;
    }
}