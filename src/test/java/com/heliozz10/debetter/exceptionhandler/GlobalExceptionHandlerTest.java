package com.heliozz10.debetter.exceptionhandler;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {
    @Test
    void dataIntegrityViolationsReturnConflictMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("That username or email is already taken.", response.getBody().get("message"));
    }

    @Test
    void illegalArgumentsReturnBadRequestMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid file extension")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid file extension", response.getBody().get("message"));
    }

    @Test
    void illegalStatesReturnBadRequestMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(
                new IllegalStateException("Tournament has already started")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tournament has already started", response.getBody().get("message"));
    }

    @Test
    void notFoundErrorsReturnNotFoundMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleNotFound(
                new EntityNotFoundException("Invitee not found")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invitee not found", response.getBody().get("message"));
    }

    @Test
    void unreadableJsonReturnsBadRequestMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleUnreadableJson(
                new HttpMessageNotReadableException("Cannot deserialize", (HttpInputMessage) null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request body.", response.getBody().get("message"));
    }
}
