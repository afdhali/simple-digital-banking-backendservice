package tutorjava.digitalbanking.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Housekeeping: expired keys are purged hourly. Safe to run on several
 * instances (idempotent DELETE).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private final IdempotencyKeyRepository repository;

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpired(Instant.now());
        log.atInfo().addKeyValue("deleted", deleted).log("Expired idempotency keys purged");
    }
}
