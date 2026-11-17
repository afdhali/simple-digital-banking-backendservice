package tutorjava.digitalbanking.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param ttl            sliding lifetime of each refresh token (renewed on
 *                       every rotation)
 * @param absoluteTtl    hard cap of a login session: the user must log in again
 *                       after this, no matter what
 * @param cookieSecure   send the cookie over HTTPS only (set false only for
 *                       plain-http local development)
 * @param cookieSameSite Strict (same-site frontend) / Lax / None (cross-site
 *                       frontend, requires secure=true)
 */
@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
        @NotNull Duration ttl,
        @NotNull Duration absoluteTtl,
        @NotBlank String cookieName,
        @NotBlank String cookiePath,
        boolean cookieSecure,
        @NotBlank String cookieSameSite) {
}
