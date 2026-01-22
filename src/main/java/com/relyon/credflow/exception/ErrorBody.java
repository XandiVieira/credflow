package com.relyon.credflow.exception;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Consistent error response body for all exception handlers.
 * Supports both single error messages and multiple validation errors.
 */
@Data
@AllArgsConstructor
public class ErrorBody {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private List<String> errors;

    /**
     * Creates an ErrorBody for a single error message.
     *
     * @param message the error message
     * @param status  HTTP status code
     * @return ErrorBody instance
     */
    public static ErrorBody from(String message, int status) {
        return new ErrorBody(LocalDateTime.now(), status, message, null);
    }

    /**
     * Creates an ErrorBody for multiple validation errors.
     *
     * @param errors list of error messages
     * @param status HTTP status code
     * @return ErrorBody instance
     */
    public static ErrorBody from(List<String> errors, int status) {
        return new ErrorBody(LocalDateTime.now(), status, null, errors);
    }

    /**
     * Creates an ErrorBody with both a message and multiple errors.
     *
     * @param message main error message
     * @param errors  list of additional error messages
     * @param status  HTTP status code
     * @return ErrorBody instance
     */
    public static ErrorBody from(String message, List<String> errors, int status) {
        return new ErrorBody(LocalDateTime.now(), status, message, errors);
    }
}
