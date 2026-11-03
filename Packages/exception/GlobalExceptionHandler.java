package com.college.placement.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<?> notFound(NotFoundException e) {
		return body(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<?> noResource(NoResourceFoundException e) {
		return body(HttpStatus.NOT_FOUND, "Resource not found");
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<?> conflict(ConflictException e) {
		return body(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<?> integrity(DataIntegrityViolationException e) {
		return body(HttpStatus.CONFLICT, "Duplicate or invalid data");
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<?> invalidState(IllegalStateException e) {
		return body(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class, PropertyReferenceException.class })
	ResponseEntity<?> badInput(Exception e) {
		return body(HttpStatus.BAD_REQUEST, "Malformed request, missing parameter or invalid value");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<?> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return body(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<?> badCredentials(BadCredentialsException e) {
		return body(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<?> authentication(AuthenticationException e) {
		return body(HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<?> accessDenied(AccessDeniedException e) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken)
			return body(HttpStatus.UNAUTHORIZED, "Authentication required");
		return body(HttpStatus.FORBIDDEN, e.getMessage() == null ? "Access denied" : e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<?> validation(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(x -> errors.put(x.getField(), x.getDefaultMessage()));
		return ResponseEntity.badRequest().body(
				Map.of("timestamp", Instant.now(), "status", 400, "error", "Validation failed", "details", errors));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<?> tooLarge(MaxUploadSizeExceededException e) {
		return body(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large (maximum 5MB)");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<?> unexpected(Exception e) {
		log.error("Unexpected error", e);
		return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
	}

	private ResponseEntity<?> body(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(Map.of("timestamp", Instant.now(), "status", status.value(), "error", message));
	}
}
