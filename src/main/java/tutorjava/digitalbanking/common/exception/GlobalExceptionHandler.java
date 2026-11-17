package tutorjava.digitalbanking.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tutorjava.digitalbanking.common.response.ApiError;

import java.util.List;

/**
 * Central error translation. Extends Spring's ResponseEntityExceptionHandler so
 * framework
 * exceptions (405, 415, malformed JSON, missing header, ...) also use the same
 * ApiError shape.
 * Internal details are logged, never returned to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---------------------------------------------------------------- domain
    // errors
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        log.atWarn()
                .addKeyValue("errorCode", code.name())
                .addKeyValue("path", request.getRequestURI())
                .log("Business rule rejected the request: {}", ex.getMessage());
        return build(code, ex.getMessage(), request.getRequestURI());
    }

    // ---------------------------------------------------------------- persistence
    // errors
    /**
     * Optimistic-lock failures, deadlocks, lock timeouts: safe for the client to
     * retry.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyFailureException ex, HttpServletRequest request) {
        log.atWarn().addKeyValue("path", request.getRequestURI())
                .log("Concurrent update detected: {}", ex.getClass().getSimpleName());
        return build(ErrorCode.CONCURRENT_UPDATE, ErrorCode.CONCURRENT_UPDATE.getDefaultMessage(),
                request.getRequestURI());
    }

    /**
     * Unique/FK/CHECK violation that slipped past service-level checks. Never
     * expose SQL details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.atWarn().addKeyValue("path", request.getRequestURI())
                .log("Data integrity violation", ex);
        return build(ErrorCode.RESOURCE_CONFLICT, ErrorCode.RESOURCE_CONFLICT.getDefaultMessage(),
                request.getRequestURI());
    }

    // ---------------------------------------------------------------- security
    // (when thrown from controllers)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), request.getRequestURI());
    }

    // ---------------------------------------------------------------- catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.atError().addKeyValue("path", request.getRequestURI())
                .log("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                request.getRequestURI());
    }

    // ---------------------------------------------------------------- Spring MVC
    // exceptions
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(status, ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(), path(request), violations);
        log.atWarn().addKeyValue("path", path(request))
                .addKeyValue("violationCount", violations.size())
                .log("Request validation failed");
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    /**
     * Fallback for every other Spring MVC exception: keep status chosen by Spring,
     * unify the body.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail pd ? pd
                : ex instanceof ErrorResponse er ? er.getBody() : null;

        String message;
        String code;
        if (statusCode.is5xxServerError()) {
            log.atError().addKeyValue("path", path(request)).log("Framework error", ex);
            message = ErrorCode.INTERNAL_ERROR.getDefaultMessage();
            code = ErrorCode.INTERNAL_ERROR.name();
        } else {
            message = problem != null && problem.getDetail() != null ? problem.getDetail() : "Bad request";
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            code = resolved != null ? resolved.name() : "ERROR";
            log.atWarn().addKeyValue("path", path(request)).addKeyValue("errorCode", code)
                    .log("Request rejected: {}", ex.getClass().getSimpleName());
        }
        ApiError error = ApiError.of(statusCode, code, message, path(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    // ---------------------------------------------------------------- helpers
    private ResponseEntity<ApiError> build(ErrorCode code, String message, String path) {
        return ResponseEntity.status(code.getStatus())
                .body(ApiError.of(code.getStatus(), code.name(), message, path));
    }

    private String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}
