package com.scholarship.util;

import com.scholarship.model.Scholarship;
import com.scholarship.model.StudentProfile;

import java.util.ArrayList;
import java.util.List;

/** Compares a student's profile with a scholarship's rules. An empty list means "eligible". */
public class EligibilityChecker {

    public static List<String> check(Scholarship s, StudentProfile p) {
        List<String> problems = new ArrayList<>();

        if (s.getMinPercentage() != null) {
            if (p.getPercentage() == null) {
                problems.add("Enter your percentage in My Profile (minimum required: " + s.getMinPercentage() + "%).");
            } else if (p.getPercentage() < s.getMinPercentage()) {
                problems.add("Your percentage (" + p.getPercentage() + "%) is below the minimum "
                        + s.getMinPercentage() + "%.");
            }
        }

        if (s.getIncomeLimit() != null) {
            if (p.getFamilyIncome() == null) {
                problems.add("Enter your family income in My Profile (limit: " + s.getIncomeLimit() + ").");
            } else if (p.getFamilyIncome() > s.getIncomeLimit()) {
                problems.add("Your family income (" + p.getFamilyIncome() + ") is above the limit "
                        + s.getIncomeLimit() + ".");
            }
        }

        if (isRestricted(s.getEligibleCourse())) {
            if (isBlank(p.getCourse())) {
                problems.add("Enter your course in My Profile (required: " + s.getEligibleCourse() + ").");
            } else if (!p.getCourse().trim().equalsIgnoreCase(s.getEligibleCourse().trim())) {
                problems.add("This scholarship is only for course: " + s.getEligibleCourse() + ".");
            }
        }

        if (isRestricted(s.getEducationLevel())) {
            if (isBlank(p.getEducationLevel())) {
                problems.add("Select your education level in My Profile (required: " + s.getEducationLevel() + ").");
            } else if (!p.getEducationLevel().equalsIgnoreCase(s.getEducationLevel())) {
                problems.add("This scholarship is only for education level: " + s.getEducationLevel() + ".");
            }
        }
        return problems;
    }

    private static boolean isRestricted(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("Any");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}