package tutorjava.digitalbanking.common.exception;

import lombok.Getter;

/**
 * Expected, domain-level failure (not a bug). Stack trace is disabled because
 * these
 * are control-flow exceptions and filling it in is wasted work.
 * Being a RuntimeException it triggers a rollback of the surrounding
 * transaction.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }
}
