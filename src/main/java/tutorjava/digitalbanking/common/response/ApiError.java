package tutorjava.digitalbanking.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import tutorjava.digitalbanking.logging.RequestLoggingFilter;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope for every non-2xx response (including 401/403 from the
 * security filters).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Standard error response")
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        @Schema(example = "INSUFFICIENT_BALANCE") String code,
        String message,
        String path,
        @Schema(description = "Correlates with the X-Request-Id header and server logs") String requestId,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(HttpStatusCode status, String code, String message, String path,
            List<FieldViolation> violations) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new ApiError(Instant.now(), status.value(), reason, code, message, path,
                MDC.get(RequestLoggingFilter.MDC_REQUEST_ID), violations);
    }

    public static ApiError of(HttpStatusCode status, String code, String message, String path) {
        return of(status, code, message, path, List.of());
    }
}
