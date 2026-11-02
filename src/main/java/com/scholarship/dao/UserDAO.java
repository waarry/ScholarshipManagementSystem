package com.scholarship.dao;

import com.scholarship.model.User;
import com.scholarship.util.DBConnection;
import com.scholarship.util.PasswordUtil;

import java.sql.*;

public class UserDAO {

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("user_id"), rs.getString("full_name"),
                            rs.getString("email"), rs.getString("password_hash"),
                            rs.getString("role"), rs.getBoolean("active"));
                }
            }
        }
        return null;
    }

    public User authenticate(String email, String password) throws SQLException {
        User u = findByEmail(email);
        if (u != null && u.isActive() && PasswordUtil.matches(password, u.getPasswordHash())) {
            return u;
        }
        return null;
    }

    public boolean registerStudent(String name, String email, String password) throws SQLException {
        if (findByEmail(email) != null) return false;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'STUDENT')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, PasswordUtil.hash(password));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO student_profiles(student_id) VALUES(?)")) {
                    ps.setInt(1, newId);
                    ps.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void ensureDefaultAdmin() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'ADMIN')")) {
                    ps.setString(1, "System Administrator");
                    ps.setString(2, "admin@college.edu");
                    ps.setString(3, PasswordUtil.hash("Admin@123"));
                    ps.executeUpdate();
                }
            }
        }
    }
}