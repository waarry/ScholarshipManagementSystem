package com.scholarship.util;

import com.scholarship.model.User;

public class Session {
    private static User currentUser;

    public static void login(User u) { currentUser = u; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
}