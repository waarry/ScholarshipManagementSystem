package com.scholarship.dao;

import com.scholarship.model.StudentProfile;
import com.scholarship.util.DBConnection;

import java.sql.*;

public class ProfileDAO {

    public StudentProfile get(int studentId) throws SQLException {
        StudentProfile p = new StudentProfile();
        p.setStudentId(studentId);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM student_profiles WHERE student_id=?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setPhone(rs.getString("phone"));
                    Date dob = rs.getDate("date_of_birth");
                    if (dob != null) p.setDateOfBirth(dob.toLocalDate());
                    p.setCourse(rs.getString("course"));
                    p.setEducationLevel(rs.getString("education_level"));
                    double pct = rs.getDouble("percentage");
                    p.setPercentage(rs.wasNull() ? null : pct);
                    double inc = rs.getDouble("family_income");
                    p.setFamilyIncome(rs.wasNull() ? null : inc);
                    p.setAddress(rs.getString("address"));
                }
            }
        }
        return p;
    }

    public void save(StudentProfile p, String fullName) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("UPDATE users SET full_name=? WHERE user_id=?")) {
                    ps.setString(1, fullName);
                    ps.setInt(2, p.getStudentId());
                    ps.executeUpdate();
                }
                String sql = "INSERT INTO student_profiles(student_id,phone,date_of_birth,course,education_level,"
                        + "percentage,family_income,address) VALUES(?,?,?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE phone=VALUES(phone), date_of_birth=VALUES(date_of_birth), "
                        + "course=VALUES(course), education_level=VALUES(education_level), "
                        + "percentage=VALUES(percentage), family_income=VALUES(family_income), address=VALUES(address)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, p.getStudentId());
                    ps.setString(2, p.getPhone());
                    if (p.getDateOfBirth() == null) ps.setNull(3, Types.DATE);
                    else ps.setDate(3, Date.valueOf(p.getDateOfBirth()));
                    ps.setString(4, p.getCourse());
                    ps.setString(5, p.getEducationLevel());
                    if (p.getPercentage() == null) ps.setNull(6, Types.DECIMAL);
                    else ps.setDouble(6, p.getPercentage());
                    if (p.getFamilyIncome() == null) ps.setNull(7, Types.DECIMAL);
                    else ps.setDouble(7, p.getFamilyIncome());
                    ps.setString(8, p.getAddress());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}