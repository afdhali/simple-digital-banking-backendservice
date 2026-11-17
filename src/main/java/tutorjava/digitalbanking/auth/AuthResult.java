package tutorjava.digitalbanking.auth;

import tutorjava.digitalbanking.security.IssuedToken;

/**
 * What the service layer returns; the controller decides HOW the refresh token
 * is delivered (cookie/body).
 */
public record AuthResult(IssuedToken accessToken, IssuedRefreshToken refreshToken) {
}
