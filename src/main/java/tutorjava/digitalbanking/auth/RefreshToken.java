package tutorjava.digitalbanking.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tutorjava.digitalbanking.common.entity.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores only the hash of the token. revoked_at is set through repository bulk
 * updates (atomic).
 */
@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "session_started_at", nullable = false, updatable = false)
    private Instant sessionStartedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static RefreshToken create(UUID userId, UUID familyId, String tokenHash,
            Instant sessionStartedAt, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.userId = userId;
        token.familyId = familyId;
        token.tokenHash = tokenHash;
        token.sessionStartedAt = sessionStartedAt;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
