package tutorjava.digitalbanking.transaction;

import tutorjava.digitalbanking.transaction.dto.TransactionResponse;

/** @param replayed true when the response was served from a previous request with the same Idempotency-Key */
public record TransactionResult(TransactionResponse response, boolean replayed) {
}
