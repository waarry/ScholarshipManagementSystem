package com.scholarship.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test
    void hashIsNotPlainTextAndMatches() {
        String hash = PasswordUtil.hash("Secret123");
        assertNotEquals("Secret123", hash);
        assertTrue(PasswordUtil.matches("Secret123", hash));
        assertFalse(PasswordUtil.matches("wrong", hash));
    }

    @Test
    void validatorRules() {
        assertTrue(Validator.isValidEmail("a@b.com"));
        assertFalse(Validator.isValidEmail("not-an-email"));
        assertTrue(Validator.isStrongPassword("Abcdef12"));
        assertFalse(Validator.isStrongPassword("short1"));
        assertFalse(Validator.isStrongPassword("onlyletters"));
    }
}