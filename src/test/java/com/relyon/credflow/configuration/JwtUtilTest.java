package com.relyon.credflow.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField(jwtUtil, "SECRET_KEY", "this-is-a-very-secure-secret-key-for-testing-purposes");
        setField(jwtUtil, "EXPIRATION_MS", 3600000L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = JwtUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void validateConfiguration_validConfig_doesNotThrow() {
        assertDoesNotThrow(() -> jwtUtil.validateConfiguration());
    }

    @Test
    void validateConfiguration_nullSecret_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", null);

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("JWT secret is not configured"));
    }

    @Test
    void validateConfiguration_blankSecret_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "   ");

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("JWT secret is not configured"));
    }

    @Test
    void validateConfiguration_shortSecret_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "short");

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }

    @Test
    void validateConfiguration_validLongSecret_doesNotThrow() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "this-is-a-valid-long-secret-key!");

        assertDoesNotThrow(() -> jwtUtil.validateConfiguration());
    }

    @Test
    void validateConfiguration_secretIsTest_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "this-is-a-very-secure-secret-key-for-testing-purposes");
        setField(jwtUtil, "EXPIRATION_MS", 0L);

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("JWT expiration must be greater than 0"));
    }

    @Test
    void validateConfiguration_negativeExpiration_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "EXPIRATION_MS", -1000L);

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("JWT expiration must be greater than 0"));
    }

    @Test
    void generateToken_validEmail_returnsNonEmptyToken() {
        var token = jwtUtil.generateToken("test@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.contains("."));
    }

    @Test
    void generateToken_differentEmails_returnsDifferentTokens() {
        var token1 = jwtUtil.generateToken("user1@example.com");
        var token2 = jwtUtil.generateToken("user2@example.com");

        assertNotEquals(token1, token2);
    }

    @Test
    void extractUsername_validToken_returnsOriginalEmail() {
        var email = "test@example.com";
        var token = jwtUtil.generateToken(email);

        var extractedEmail = jwtUtil.extractUsername(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        var token = jwtUtil.generateToken("test@example.com");

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        var token = jwtUtil.generateToken("test@example.com");
        var tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        assertFalse(jwtUtil.isTokenValid(tamperedToken));
    }

    @Test
    void isTokenValid_emptyToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.isTokenValid(""));
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not-a-jwt-token"));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() throws Exception {
        setField(jwtUtil, "EXPIRATION_MS", 1L);
        var token = jwtUtil.generateToken("test@example.com");
        Thread.sleep(10);

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void extractUsername_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.extractUsername("invalid.token"));
    }

    @Test
    void generateAndValidate_roundTrip_worksCorrectly() {
        var email = "roundtrip@example.com";

        var token = jwtUtil.generateToken(email);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals(email, jwtUtil.extractUsername(token));
    }

    @Test
    void validateConfiguration_validSecretWithSpecialChars_doesNotThrow() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "v@lid-secret-with-special-chars!");

        assertDoesNotThrow(() -> jwtUtil.validateConfiguration());
    }

    @Test
    void validateConfiguration_secretValue_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "secretvalueistooshorttopass!");

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }

    @Test
    void validateConfiguration_testValue_throwsIllegalStateException() throws Exception {
        setField(jwtUtil, "SECRET_KEY", "testisalsotooshorttopasscheck!");

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtil.validateConfiguration());
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }
}
