package com.edu.course.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResourceNotFoundException - Returns 404")
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Course", "id", 1L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("Course");
    }

    @Test
    @DisplayName("handleBadRequestException - Returns 400")
    void handleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid request");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    @DisplayName("handleForbiddenException - Returns 403")
    void handleForbiddenException() {
        ForbiddenException ex = new ForbiddenException("Access denied");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleForbiddenException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("handleValidationExceptions - Returns 400 with field errors")
    void handleValidationExceptions() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "object");
        FieldError fieldError = new FieldError("object", "title", "must not be blank");
        bindingResult.addError(fieldError);
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyValidation", Object.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    @DisplayName("handleValidationExceptions - Handles empty errors list")
    void handleValidationExceptions_EmptyErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyValidation", Object.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    @DisplayName("handleGlobalException - Returns 500")
    void handleGlobalException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGlobalException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).contains("unexpected error");
    }

    @Test
    @DisplayName("ResourceNotFoundException - Contains resource details")
    void resourceNotFoundException_ContainsDetails() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Category", "name", "Programming");

        assertThat(ex.getResourceName()).isEqualTo("Category");
        assertThat(ex.getFieldName()).isEqualTo("name");
        assertThat(ex.getFieldValue()).isEqualTo("Programming");
        assertThat(ex.getMessage()).contains("Category").contains("name").contains("Programming");
    }

    private void dummyValidation(Object payload) {
    }
}
