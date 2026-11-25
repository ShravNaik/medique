package com.medique.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    @Test
    public void testSaltGeneration() {
        String salt1 = PasswordHasher.generateSalt();
        String salt2 = PasswordHasher.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2, "Salts must be randomly unique");
    }

    @Test
    public void testPasswordHashAndVerification() {
        String rawPassword = "SecurePassword2026!";
        String salt = PasswordHasher.generateSalt();

        String hash = PasswordHasher.hashPassword(rawPassword, salt);
        assertNotNull(hash);

        assertTrue(PasswordHasher.verifyPassword(rawPassword, salt, hash), "Password verification should pass");
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", salt, hash), "Incorrect password must fail");
        assertFalse(PasswordHasher.verifyPassword(rawPassword, "differentSalt", hash), "Different salt must fail");
    }
}
