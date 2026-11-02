package com.scholarship.util;

import com.scholarship.model.Scholarship;
import com.scholarship.model.StudentProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityCheckerTest {

    private Scholarship rules() {
        Scholarship s = new Scholarship();
        s.setMinPercentage(60.0);
        s.setIncomeLimit(300000.0);
        s.setEligibleCourse("BCA");
        s.setEducationLevel("Any");
        return s;
    }

    private StudentProfile goodStudent() {
        StudentProfile p = new StudentProfile();
        p.setPercentage(75.0);
        p.setFamilyIncome(200000.0);
        p.setCourse("bca");
        return p;
    }

    @Test
    void eligibleStudentHasNoProblems() {
        assertTrue(EligibilityChecker.check(rules(), goodStudent()).isEmpty());
    }

    @Test
    void lowPercentageIsRejected() {
        StudentProfile p = goodStudent();
        p.setPercentage(50.0);
        assertEquals(1, EligibilityChecker.check(rules(), p).size());
    }

    @Test
    void highIncomeIsRejected() {
        StudentProfile p = goodStudent();
        p.setFamilyIncome(500000.0);
        assertEquals(1, EligibilityChecker.check(rules(), p).size());
    }

    @Test
    void wrongCourseIsRejected() {
        StudentProfile p = goodStudent();
        p.setCourse("BBA");
        assertEquals(1, EligibilityChecker.check(rules(), p).size());
    }

    @Test
    void emptyProfileListsMissingDetails() {
        assertEquals(3, EligibilityChecker.check(rules(), new StudentProfile()).size());
    }
}