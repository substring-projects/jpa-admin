package com.substringtechnologies.jpaadmin.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raised when submitted data fails bean validation or cannot be converted. Keys are field paths such as
 * {@code price} or {@code items[0].quantity} so clients can show each message next to its input.
 */
public class AdminValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public AdminValidationException(Map<String, String> fieldErrors) {
        super("Please correct the highlighted fields");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
