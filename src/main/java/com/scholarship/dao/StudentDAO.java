package com.scholarship.dao;

import com.scholarship.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only list of registered students for the admin. */
public class StudentDAO {

    /** Each row: {id, name, email, phone, course, level, percentage, income, applications, joined}. */
    public List<String[]> findAll() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.full_name, u.email, u.created_at, "
                + "p.phone, p.course, p.education_level, p.percentage, p.family_income, "
                + "(SELECT COUNT(*) FROM applications a WHERE a.student_id=u.user_id AND a.status<>'DRAFT') AS apps "
                + "FROM users u LEFT JOIN student_profiles p ON u.user_id=p.student_id "
                + "WHERE u.role='STUDENT' ORDER BY u.user_id DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        nz(rs.getString("phone")),
                        nz(rs.getString("course")),
                        nz(rs.getString("education_level")),
                        nz(rs.getString("percentage")),
                        nz(rs.getString("family_income")),
                        String.valueOf(rs.getInt("apps")),
                        rs.getTimestamp("created_at").toString().substring(0, 10)});
            }
        }
        return list;
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}