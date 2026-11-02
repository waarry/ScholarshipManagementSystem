package com.scholarship.util;

/** The rules for which status changes an administrator may make. Kept separate so it is easy to test. */
public class StatusRules {

    public static boolean isAllowed(String from, String to) {
        return switch (to) {
            case "UNDER_REVIEW" -> from.equals("SUBMITTED");
            case "DOCUMENTS_REQUESTED" -> from.equals("SUBMITTED") || from.equals("UNDER_REVIEW");
            case "APPROVED", "REJECTED" -> from.equals("UNDER_REVIEW");
            default -> false;
        };
    }
}