package tutorjava.digitalbanking.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tutorjava.digitalbanking.config.JwtProperties;
import tutorjava.digitalbanking.user.User;
import tutorjava.digitalbanking.user.UserRole;

import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates HS256 access tokens (Nimbus JOSE+JWT).
 * Validation pins the algorithm, verifies the signature, issuer and expiry.
 * Fails fast at startup when the secret is missing, not base64 or shorter than
 * 256 bits.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        try {
            byte[] secret = Base64.getDecoder().decode(properties.secret());
            if (secret.length < 32) {
                throw new IllegalStateException("app.jwt.secret must decode to at least 32 bytes (256 bits)");
            }
            this.signer = new MACSigner(secret);
            this.verifier = new MACVerifier(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwt.secret must be base64 encoded (openssl rand -base64 48)", e);
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to initialise JWT signer/verifier", e);
        }
    }

    public IssuedToken generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenExpiration());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return new IssuedToken(jwt.serialize(), properties.accessTokenExpiration().toSeconds());
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    /**
     * Returns the principal for a valid token, or empty for anything wrong (never
     * throws).
     */
    public Optional<AuthenticatedUser> parseAndValidate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(verifier)) {
                log.debug("JWT rejected: bad algorithm or signature");
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (!properties.issuer().equals(claims.getIssuer())
                    || expiration == null
                    || expiration.toInstant().isBefore(Instant.now())) {
                log.debug("JWT rejected: bad issuer or expired");
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.getStringClaim(CLAIM_EMAIL),
                    UserRole.valueOf(claims.getStringClaim(CLAIM_ROLE))));
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException e) {
            log.debug("JWT rejected: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
