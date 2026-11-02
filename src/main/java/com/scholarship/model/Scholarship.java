package com.scholarship.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One scholarship program. Min percentage and income limit are optional (null = no rule). */
public class Scholarship {
    private int id;
    private String name;
    private String description;
    private String sponsor;
    private String eligibility;
    private Double minPercentage;
    private Double incomeLimit;
    private String eligibleCourse;
    private String educationLevel;
    private double amount;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String status = "DRAFT";
    private List<String> requiredDocs = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSponsor() { return sponsor; }
    public void setSponsor(String sponsor) { this.sponsor = sponsor; }
    public String getEligibility() { return eligibility; }
    public void setEligibility(String eligibility) { this.eligibility = eligibility; }
    public Double getMinPercentage() { return minPercentage; }
    public void setMinPercentage(Double minPercentage) { this.minPercentage = minPercentage; }
    public Double getIncomeLimit() { return incomeLimit; }
    public void setIncomeLimit(Double incomeLimit) { this.incomeLimit = incomeLimit; }
    public String getEligibleCourse() { return eligibleCourse; }
    public void setEligibleCourse(String eligibleCourse) { this.eligibleCourse = eligibleCourse; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getRequiredDocs() { return requiredDocs; }
    public void setRequiredDocs(List<String> requiredDocs) { this.requiredDocs = requiredDocs; }
}