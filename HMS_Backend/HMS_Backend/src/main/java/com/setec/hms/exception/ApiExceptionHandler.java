package com.setec.hms.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Maps exceptions to HTTP statuses. Unknown errors are logged and returned as a generic 500 so
 * internal details are not leaked to the client.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  record Error(LocalDateTime timestamp, int status, String error, String message, String path) {}

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Error> notFound(NotFoundException e, HttpServletRequest r) {
    return out(HttpStatus.NOT_FOUND, e.getMessage(), r);
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<Error> conflict(ConflictException e, HttpServletRequest r) {
    return out(HttpStatus.CONFLICT, e.getMessage(), r);
  }

  /** Foreign-key and unique-constraint violations, e.g. deleting a record that is still in use. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Error> dataIntegrity(DataIntegrityViolationException e, HttpServletRequest r) {
    log.warn("Data integrity violation on {}: {}", r.getRequestURI(), e.getMostSpecificCause().getMessage());
    return out(
        HttpStatus.CONFLICT,
        "This change conflicts with existing records (a duplicate value, or the record is still in use).",
        r);
  }

  @ExceptionHandler(DisabledException.class)
  ResponseEntity<Error> disabled(DisabledException e, HttpServletRequest r) {
    return out(HttpStatus.FORBIDDEN, "This account is disabled. Contact an administrator.", r);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Error> unauthenticated(AuthenticationException e, HttpServletRequest r) {
    return out(HttpStatus.UNAUTHORIZED, "Invalid username/email or password.", r);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Error> forbidden(AccessDeniedException e, HttpServletRequest r) {
    return out(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", r);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Error> largeUpload(MaxUploadSizeExceededException e, HttpServletRequest r) {
    return out(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.", r);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Error> invalidRequest(MethodArgumentNotValidException e, HttpServletRequest r) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse("Request validation failed");
    return out(HttpStatus.BAD_REQUEST, message, r);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Error> unreadableRequest(HttpMessageNotReadableException e, HttpServletRequest r) {
    return out(
        HttpStatus.BAD_REQUEST,
        "Request body contains an invalid value. Check the allowed values in the API schema.",
        r);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<Error> badParameter(MethodArgumentTypeMismatchException e, HttpServletRequest r) {
    return out(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'.", r);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<Error> missingParameter(
      MissingServletRequestParameterException e, HttpServletRequest r) {
    return out(HttpStatus.BAD_REQUEST, "Missing parameter '" + e.getParameterName() + "'.", r);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  ResponseEntity<Error> badSort(PropertyReferenceException e, HttpServletRequest r) {
    return out(HttpStatus.BAD_REQUEST, "Unknown sort field '" + e.getPropertyName() + "'.", r);
  }

  /** Hibernate reports unknown sort/property names this way; it is a client error. */
  @ExceptionHandler(InvalidDataAccessApiUsageException.class)
  ResponseEntity<Error> invalidQuery(InvalidDataAccessApiUsageException e, HttpServletRequest r) {
    log.warn("Rejected query on {}: {}", r.getRequestURI(), e.getMostSpecificCause().getMessage());
    return out(HttpStatus.BAD_REQUEST, "Invalid sort field or query parameter.", r);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Error> badArgument(IllegalArgumentException e, HttpServletRequest r) {
    return out(HttpStatus.BAD_REQUEST, e.getMessage(), r);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Error> unexpected(Exception e, HttpServletRequest r) {
    log.error("Unhandled error on {}", r.getRequestURI(), e);
    return out(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.", r);
  }

  private ResponseEntity<Error> out(HttpStatus s, String message, HttpServletRequest r) {
    return ResponseEntity.status(s)
        .body(new Error(LocalDateTime.now(), s.value(), s.getReasonPhrase(), message, r.getRequestURI()));
  }
}
