package tutorjava.digitalbanking.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Atomic "claim": returns 1 when this request won the key, 0 when the key
     * already exists.
     * If another in-flight transaction holds the same key, PostgreSQL makes this
     * statement wait
     * until that transaction commits or rolls back, which serialises concurrent
     * duplicates.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (id, user_id, idempotency_key, request_hash, created_at, expires_at)
            VALUES (:id, :userId, :key, :requestHash, :createdAt, :expiresAt)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("key") String key,
            @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt,
            @Param("expiresAt") Instant expiresAt);

    Optional<IdempotencyKey> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Modifying
    @Query("update IdempotencyKey k set k.transactionId = :transactionId where k.id = :id")
    int attachTransaction(@Param("id") UUID id, @Param("transactionId") UUID transactionId);

    @Modifying
    @Query("delete from IdempotencyKey k where k.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
