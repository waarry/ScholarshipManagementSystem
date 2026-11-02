package com.scholarship.util;

public class Validator {
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    public static boolean isStrongPassword(String pw) {
        return pw != null && pw.length() >= 8 && pw.matches(".*[A-Za-z].*") && pw.matches(".*\\d.*");
    }
}