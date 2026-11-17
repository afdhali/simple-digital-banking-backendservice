package tutorjava.digitalbanking.security;

public record IssuedToken(String value, long expiresInSeconds) {
}
