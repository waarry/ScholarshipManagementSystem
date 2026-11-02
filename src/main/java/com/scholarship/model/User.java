package com.scholarship.model;

public class User {
    private final int id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public User(int id, String fullName, String email, String passwordHash, String role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}