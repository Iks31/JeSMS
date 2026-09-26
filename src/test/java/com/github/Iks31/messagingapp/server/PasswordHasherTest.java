package com.github.Iks31.messagingapp.server;

import com.github.Iks31.messagingapp.server.db.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hashVerifiesOnlyTheOriginalPassword() {
        String hash = PasswordHasher.hash("secret123");
        assertTrue(PasswordHasher.verify("secret123", hash));
        assertFalse(PasswordHasher.verify("secret124", hash));
        assertFalse(PasswordHasher.isLegacy(hash));
    }

    @Test
    void hashesAreSalted() {
        assertNotEquals(PasswordHasher.hash("secret123"), PasswordHasher.hash("secret123"));
    }

    @Test
    void legacyPlainTextPasswordsStillVerify() {
        assertTrue(PasswordHasher.isLegacy("hunter2"));
        assertTrue(PasswordHasher.verify("hunter2", "hunter2"));
        assertFalse(PasswordHasher.verify("hunter3", "hunter2"));
    }

    @Test
    void malformedHashesAndNullsAreRejected() {
        assertFalse(PasswordHasher.verify("x", "pbkdf2$notanumber$abc$def"));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("x")));
        assertFalse(PasswordHasher.verify("x", null));
    }
}
