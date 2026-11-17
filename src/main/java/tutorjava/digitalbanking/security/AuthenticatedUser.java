package tutorjava.digitalbanking.security;

import tutorjava.digitalbanking.user.UserRole;

import java.util.UUID;

/**
 * Principal placed in the SecurityContext after a JWT was verified. Resolve
 * with @AuthenticationPrincipal.
 */
public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
