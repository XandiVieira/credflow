package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorBodyTest {

    @Test
    void from_singleMessage_createsErrorBodyWithMessageOnly() {
        var before = LocalDateTime.now();

        var result = ErrorBody.from("Error occurred", 400);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Error occurred", result.getMessage());
        assertNull(result.getErrors());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(result.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void from_nullMessage_handlesNullGracefully() {
        var result = ErrorBody.from((String) null, 500);

        assertNotNull(result);
        assertEquals(500, result.getStatus());
        assertNull(result.getMessage());
        assertNull(result.getErrors());
    }

    @Test
    void from_errorsList_createsErrorBodyWithErrorsOnly() {
        var errors = List.of("Field 1 is invalid", "Field 2 is required");

        var result = ErrorBody.from(errors, 400);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNull(result.getMessage());
        assertNotNull(result.getErrors());
        assertEquals(2, result.getErrors().size());
        assertEquals("Field 1 is invalid", result.getErrors().get(0));
        assertEquals("Field 2 is required", result.getErrors().get(1));
    }

    @Test
    void from_emptyErrorsList_createsErrorBodyWithEmptyList() {
        var result = ErrorBody.from(List.of(), 400);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNull(result.getMessage());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void from_messageAndErrors_createsBothFields() {
        var errors = List.of("Validation error 1", "Validation error 2");

        var result = ErrorBody.from("Validation failed", errors, 422);

        assertNotNull(result);
        assertEquals(422, result.getStatus());
        assertEquals("Validation failed", result.getMessage());
        assertNotNull(result.getErrors());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void from_messageAndNullErrors_handlesBothGracefully() {
        var result = ErrorBody.from("Error", null, 400);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Error", result.getMessage());
        assertNull(result.getErrors());
    }

    @Test
    void from_nullMessageAndErrors_handlesBothGracefully() {
        var errors = List.of("Error 1");

        var result = ErrorBody.from(null, errors, 400);

        assertNotNull(result);
        assertNull(result.getMessage());
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void from_differentStatusCodes_setsCorrectly() {
        var badRequest = ErrorBody.from("Bad Request", 400);
        var unauthorized = ErrorBody.from("Unauthorized", 401);
        var notFound = ErrorBody.from("Not Found", 404);
        var conflict = ErrorBody.from("Conflict", 409);
        var serverError = ErrorBody.from("Server Error", 500);

        assertEquals(400, badRequest.getStatus());
        assertEquals(401, unauthorized.getStatus());
        assertEquals(404, notFound.getStatus());
        assertEquals(409, conflict.getStatus());
        assertEquals(500, serverError.getStatus());
    }

    @Test
    void constructor_allArgs_setsAllFields() {
        var timestamp = LocalDateTime.now();
        var errors = List.of("error1", "error2");

        var result = new ErrorBody(timestamp, 422, "Main error", errors);

        assertEquals(timestamp, result.getTimestamp());
        assertEquals(422, result.getStatus());
        assertEquals("Main error", result.getMessage());
        assertEquals(errors, result.getErrors());
    }

    @Test
    void setters_modifyFields() {
        var errorBody = ErrorBody.from("Initial", 400);
        var newTimestamp = LocalDateTime.now().minusDays(1);
        var newErrors = List.of("new error");

        errorBody.setTimestamp(newTimestamp);
        errorBody.setStatus(500);
        errorBody.setMessage("Updated");
        errorBody.setErrors(newErrors);

        assertEquals(newTimestamp, errorBody.getTimestamp());
        assertEquals(500, errorBody.getStatus());
        assertEquals("Updated", errorBody.getMessage());
        assertEquals(newErrors, errorBody.getErrors());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        var timestamp = LocalDateTime.of(2024, 1, 1, 12, 0);
        var errors = List.of("error");

        var body1 = new ErrorBody(timestamp, 400, "message", errors);
        var body2 = new ErrorBody(timestamp, 400, "message", errors);

        assertEquals(body1, body2);
    }

    @Test
    void hashCode_sameValues_returnsSameHashCode() {
        var timestamp = LocalDateTime.of(2024, 1, 1, 12, 0);
        var errors = List.of("error");

        var body1 = new ErrorBody(timestamp, 400, "message", errors);
        var body2 = new ErrorBody(timestamp, 400, "message", errors);

        assertEquals(body1.hashCode(), body2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        var body = ErrorBody.from("Test message", 400);

        var result = body.toString();

        assertTrue(result.contains("400"));
        assertTrue(result.contains("Test message"));
    }
}
