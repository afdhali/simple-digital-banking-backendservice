package tutorjava.digitalbanking.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.util.Sha256;
import tutorjava.digitalbanking.config.RefreshTokenProperties;
import tutorjava.digitalbanking.user.User;
import tutorjava.digitalbanking.user.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh-token lifecycle with rotation and reuse detection.
 *
 * - login -> new family (a "session") with its first token
 * - refresh -> the presented token is atomically revoked and replaced by a new
 * one in the same family
 * - reuse -> presenting a token that was already used/revoked means it was
 * probably stolen:
 * the whole family is revoked, so the thief AND the victim must log in again
 * - logout -> the whole family is revoked
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32; // 256 bits of entropy

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final RefreshTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public record RotatedSession(User user, IssuedRefreshToken refreshToken) {
    }

    @Transactional
    public IssuedRefreshToken startSession(UUID userId) {
        Instant now = Instant.now();
        return issue(userId, UUID.randomUUID(), now, now);
    }

    /**
     * noRollbackFor: when reuse is detected we revoke the family and THEN throw
     * 401. The revocation
     * must be committed, so this exception must not roll the transaction back.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public RotatedSession rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken stored = repository.findByTokenHash(Sha256.hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.getRevokedAt() != null) {
            throw reuseDetected(stored, now);
        }
        if (stored.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // Atomic claim: if two requests present the same token concurrently only one
        // gets 1 here.
        if (repository.revokeIfActive(stored.getId(), now) == 0) {
            throw reuseDetected(stored, now);
        }

        User user = userRepository.findById(stored.getUserId()).filter(User::isEnabled).orElse(null);
        if (user == null) {
            repository.revokeFamily(stored.getFamilyId(), now);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        IssuedRefreshToken next = issue(user.getId(), stored.getFamilyId(), stored.getSessionStartedAt(), now);
        log.atInfo().addKeyValue("userId", user.getId()).log("Refresh token rotated");
        return new RotatedSession(user, next);
    }

    /** Idempotent: unknown/expired/already revoked tokens are silently ignored. */
    @Transactional
    public void revokeSession(String rawToken) {
        repository.findByTokenHash(Sha256.hex(rawToken)).ifPresent(token -> {
            repository.revokeFamily(token.getFamilyId(), Instant.now());
            log.atInfo().addKeyValue("userId", token.getUserId()).log("Session revoked (logout)");
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        int revoked = repository.revokeAllForUser(userId, Instant.now());
        log.atInfo().addKeyValue("userId", userId).addKeyValue("revokedTokens", revoked)
                .log("All sessions revoked (logout everywhere)");
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId, Instant sessionStartedAt, Instant now) {
        Instant slidingExpiry = now.plus(properties.ttl());
        Instant absoluteExpiry = sessionStartedAt.plus(properties.absoluteTtl());
        Instant expiresAt = slidingExpiry.isBefore(absoluteExpiry) ? slidingExpiry : absoluteExpiry;

        String raw = generateRawToken();
        repository.save(RefreshToken.create(userId, familyId, Sha256.hex(raw), sessionStartedAt, expiresAt));
        return new IssuedRefreshToken(raw, expiresAt);
    }

    private BusinessException reuseDetected(RefreshToken stored, Instant now) {
        repository.revokeFamily(stored.getFamilyId(), now);
        log.atWarn()
                .addKeyValue("userId", stored.getUserId())
                .addKeyValue("familyId", stored.getFamilyId())
                .log("Refresh token reuse detected: session family revoked");
        return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
