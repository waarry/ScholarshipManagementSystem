package com.scholarship.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusRulesTest {
    @Test
    void normalFlowIsAllowed() {
        assertTrue(StatusRules.isAllowed("SUBMITTED", "UNDER_REVIEW"));
        assertTrue(StatusRules.isAllowed("UNDER_REVIEW", "APPROVED"));
        assertTrue(StatusRules.isAllowed("UNDER_REVIEW", "REJECTED"));
        assertTrue(StatusRules.isAllowed("UNDER_REVIEW", "DOCUMENTS_REQUESTED"));
        assertTrue(StatusRules.isAllowed("SUBMITTED", "DOCUMENTS_REQUESTED"));
    }

    @Test
    void cannotSkipReview() {
        assertFalse(StatusRules.isAllowed("SUBMITTED", "APPROVED"));
        assertFalse(StatusRules.isAllowed("SUBMITTED", "REJECTED"));
    }

    @Test
    void finalDecisionsCannotBeChanged() {
        assertFalse(StatusRules.isAllowed("APPROVED", "REJECTED"));
        assertFalse(StatusRules.isAllowed("REJECTED", "APPROVED"));
        assertFalse(StatusRules.isAllowed("APPROVED", "UNDER_REVIEW"));
    }

    @Test
    void waitingForStudentCannotBeDecided() {
        assertFalse(StatusRules.isAllowed("DOCUMENTS_REQUESTED", "APPROVED"));
    }
}