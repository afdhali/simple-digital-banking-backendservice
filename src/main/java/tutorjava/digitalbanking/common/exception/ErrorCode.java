package tutorjava.digitalbanking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Single source of truth for machine-readable error codes returned to API
 * clients.
 */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST,
            "Idempotency-Key must be 8-100 characters (letters, digits, '_', '-', '.', ':')"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required or the token is invalid"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired or already used"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    USER_LOCKED(HttpStatus.LOCKED, "Too many failed login attempts. Please try again later"),

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Account not found"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Transaction not found"),

    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered"),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "The resource was modified concurrently. Please retry"),
    IDEMPOTENCY_IN_PROGRESS(HttpStatus.CONFLICT, "A request with this Idempotency-Key is still being processed"),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource"),

    ACCOUNT_LIMIT_REACHED(HttpStatusCode.valueOf(422), "Maximum number of accounts reached"),
    ACCOUNT_NOT_ACTIVE(HttpStatusCode.valueOf(422), "Account is not active"),
    INSUFFICIENT_BALANCE(HttpStatusCode.valueOf(422), "Insufficient balance"),
    SAME_ACCOUNT_TRANSFER(HttpStatusCode.valueOf(422), "Source and destination account must be different"),
    CURRENCY_MISMATCH(HttpStatusCode.valueOf(422), "Source and destination account currency differ"),
    AMOUNT_LIMIT_EXCEEDED(HttpStatusCode.valueOf(422), "Amount exceeds the maximum allowed per transaction"),
    IDEMPOTENCY_KEY_REUSED(HttpStatusCode.valueOf(422),
            "This Idempotency-Key was already used with a different request payload"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatusCode status;
    private final String defaultMessage;

    ErrorCode(HttpStatusCode status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
