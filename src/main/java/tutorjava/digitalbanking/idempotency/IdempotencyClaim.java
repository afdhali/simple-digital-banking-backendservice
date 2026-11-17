package tutorjava.digitalbanking.idempotency;

import java.util.UUID;

/** Outcome of trying to claim an Idempotency-Key. */
public sealed interface IdempotencyClaim {

    /**
     * First time we see this key: caller must execute the operation, then call
     * complete().
     */
    record Fresh(UUID claimId) implements IdempotencyClaim {
    }

    /**
     * Same key + same payload was already processed: return the stored result, do
     * nothing.
     */
    record Replay(UUID transactionId) implements IdempotencyClaim {
    }
}
