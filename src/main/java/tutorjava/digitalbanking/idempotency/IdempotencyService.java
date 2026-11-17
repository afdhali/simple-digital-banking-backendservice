package tutorjava.digitalbanking.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.util.Sha256;
import tutorjava.digitalbanking.config.IdempotencyProperties;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Exactly-once semantics for money-moving requests.
 *
 * The claim row and the business change live in ONE database transaction:
 * - success -> both commit together (key + result recorded atomically)
 * - failure -> both roll back (client may retry with the same key and it is
 * re-evaluated)
 * - duplicate (sequential or concurrent) -> the unique index makes the loser
 * wait, then replay the stored result
 * - same key with another payload -> rejected (422), protects against client
 * bugs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String HEADER = "Idempotency-Key";

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_.:-]{8,100}$");

    private final IdempotencyKeyRepository repository;
    private final IdempotencyProperties properties;

    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyClaim claim(UUID userId, String key, String requestFingerprint) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        String requestHash = Sha256.hex(requestFingerprint);
        Instant now = Instant.now();
        UUID claimId = UUID.randomUUID();

        int inserted = repository.insertIfAbsent(claimId, userId, key, requestHash, now,
                now.plus(properties.ttl()));
        if (inserted == 1) {
            return new IdempotencyClaim.Fresh(claimId);
        }

        IdempotencyKey existing = repository.findByUserIdAndIdempotencyKey(userId, key)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_IN_PROGRESS));
        if (!existing.getRequestHash().equals(requestHash)) {
            log.atWarn().addKeyValue("userId", userId).log("Idempotency-Key reused with a different payload");
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if (existing.getTransactionId() == null) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_IN_PROGRESS);
        }
        log.atInfo().addKeyValue("userId", userId)
                .addKeyValue("transactionId", existing.getTransactionId())
                .log("Idempotent replay: returning stored result");
        return new IdempotencyClaim.Replay(existing.getTransactionId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID claimId, UUID transactionId) {
        repository.attachTransaction(claimId, transactionId);
    }

}
