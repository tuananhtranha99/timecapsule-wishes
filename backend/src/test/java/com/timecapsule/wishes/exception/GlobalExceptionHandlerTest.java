package com.timecapsule.wishes.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {

        @GetMapping("/business")
        public void throwBusinessException() {
            throw new BusinessException("Invalid milestone date range");
        }

        @GetMapping("/not-found")
        public void throwResourceNotFoundException() {
            throw new ResourceNotFoundException("Recipient", "id", "123e4567-e89b-12d3-a456-426614174000");
        }

        @GetMapping("/conflict")
        public void throwConflictBusinessException() {
            throw new BusinessException("Recipient already exists with this name", HttpStatus.CONFLICT);
        }

        @GetMapping("/generic")
        public void throwGenericException() {
            throw new RuntimeException("Database connection timeout");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should return 400 Bad Request with standardized ErrorResponse when BusinessException is thrown")
    void testHandleBusinessException() throws Exception {
        mockMvc.perform(get("/test-exceptions/business")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid milestone date range")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should return 404 Not Found with standardized ErrorResponse when ResourceNotFoundException is thrown")
    void testHandleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Recipient not found with id: '123e4567-e89b-12d3-a456-426614174000'")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should return custom HTTP status (409 Conflict) when BusinessException is initialized with CONFLICT")
    void testHandleCustomStatusBusinessException() throws Exception {
        mockMvc.perform(get("/test-exceptions/conflict")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Recipient already exists with this name")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error without leaking internal exception message when unexpected error occurs")
    void testHandleGenericException() throws Exception {
        mockMvc.perform(get("/test-exceptions/generic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected internal error occurred. Please try again later.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
