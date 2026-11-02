package com.scholarship.model;

import java.time.LocalDate;

public class StudentProfile {
    private int studentId;
    private String phone;
    private LocalDate dateOfBirth;
    private String course;
    private String educationLevel;
    private Double percentage;
    private Double familyIncome;
    private String address;

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public Double getFamilyIncome() { return familyIncome; }
    public void setFamilyIncome(Double familyIncome) { this.familyIncome = familyIncome; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}