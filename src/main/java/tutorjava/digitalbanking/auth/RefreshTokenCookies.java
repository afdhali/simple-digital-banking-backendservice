package tutorjava.digitalbanking.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.config.CorsProperties;
import tutorjava.digitalbanking.config.RefreshTokenProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * Browser delivery of the refresh token: HttpOnly (invisible to JavaScript, so
 * XSS cannot steal it),
 * Secure, SameSite, and scoped to the auth path so it is not sent with ordinary
 * API calls.
 * Because cookies are sent automatically, cookie-based endpoints also verify
 * the Origin header (CSRF defence).
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookies {

    private final RefreshTokenProperties properties;
    private final CorsProperties corsProperties;

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> properties.cookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    public ResponseCookie create(IssuedRefreshToken token) {
        Duration maxAge = Duration.between(Instant.now(), token.expiresAt());
        return base(token.value()).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    /**
     * Browsers always send Origin on cross-origin POSTs. If it is present it must
     * be one of our allowed
     * origins. Requests without Origin come from non-browser clients, which are not
     * exposed to CSRF.
     */
    public void requireTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !corsProperties.allowedOrigins().contains(origin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(properties.cookiePath());
    }
}
