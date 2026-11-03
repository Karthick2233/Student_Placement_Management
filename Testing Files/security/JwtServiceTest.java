package com.college.placement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-with-more-than-32-bytes".getBytes());
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("another-test-secret-which-is-also-long-enough!!".getBytes());

    @Test
    void generatesAndValidatesToken() {
        JwtService service = new JwtService(SECRET, 60000);
        String token = service.generate("student@example.com", "STUDENT");

        assertEquals("student@example.com", service.extractEmail(token));
        assertTrue(service.isValid(token, "student@example.com"));
        assertFalse(service.isValid(token, "other@example.com"));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService service = new JwtService(SECRET, -1000);
        String token = service.generate("student@example.com", "STUDENT");

        assertFalse(service.isValid(token, "student@example.com"));
        assertThrows(JwtException.class, () -> service.extractEmail(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsInvalid() {
        String token = new JwtService(OTHER_SECRET, 60000).generate("student@example.com", "STUDENT");

        assertFalse(new JwtService(SECRET, 60000).isValid(token, "student@example.com"));
    }

    @Test
    void tamperedTokenIsInvalid() {
        JwtService service = new JwtService(SECRET, 60000);
        String token = service.generate("student@example.com", "STUDENT");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertFalse(service.isValid(tampered, "student@example.com"));
    }

    @Test
    void garbageTokenIsInvalid() {
        JwtService service = new JwtService(SECRET, 60000);

        assertFalse(service.isValid("not-a-jwt", "student@example.com"));
    }

    @Test
    void secretMustBeBase64() {
        assertThrows(IllegalStateException.class, () -> new JwtService("!!!", 60000));
    }

    @Test
    void secretMustBeAtLeast32Bytes() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThrows(IllegalStateException.class, () -> new JwtService(shortSecret, 60000));
    }
}
