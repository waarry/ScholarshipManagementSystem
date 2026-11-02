package com.scholarship;

import com.scholarship.dao.UserDAO;
import com.scholarship.ui.LoginFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        try {
            new UserDAO().ensureDefaultAdmin();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the database.\nCheck db.properties and make sure schema.sql was run.\n\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}