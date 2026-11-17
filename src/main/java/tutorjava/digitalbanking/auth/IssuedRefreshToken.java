package tutorjava.digitalbanking.auth;

import java.time.Instant;

/**
 * The raw value exists only here and in the response; the database holds just
 * its hash.
 */
public record IssuedRefreshToken(String value, Instant expiresAt) {
}
