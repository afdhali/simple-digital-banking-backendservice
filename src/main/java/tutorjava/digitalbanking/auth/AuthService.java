package tutorjava.digitalbanking.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.auth.dto.LoginRequest;
import tutorjava.digitalbanking.auth.dto.RegisterRequest;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.config.AuthProperties;
import tutorjava.digitalbanking.security.JwtService;
import tutorjava.digitalbanking.user.User;
import tutorjava.digitalbanking.user.UserRepository;
import tutorjava.digitalbanking.user.dto.UserResponse;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;

    /**
     * Used to spend the same CPU time when the email is unknown (mitigates user
     * enumeration by timing).
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, RefreshTokenService refreshTokenService,
            AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
        this.dummyHash = passwordEncoder.encode("timing-attack-mitigation-dummy");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        User user = User.register(email, request.fullName().trim(), passwordEncoder.encode(request.password()));
        try {
            // flush now so a concurrent duplicate surfaces here as a clean business error
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        log.atInfo().addKeyValue("userId", user.getId()).log("User registered");
        return UserResponse.from(user);
    }

    /**
     * Intentionally not @Transactional: the failed-attempt counter must be
     * committed
     * even though the method ends by throwing an exception.
     */
    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            log.atWarn().log("Login failed: unknown email");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        if (user.isLocked(now)) {
            log.atWarn().addKeyValue("userId", user.getId()).log("Login rejected: user temporarily locked");
            throw new BusinessException(ErrorCode.USER_LOCKED);
        }

        boolean passwordOk = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!passwordOk || !user.isEnabled()) {
            user.registerFailedLogin(authProperties.maxFailedLoginAttempts(), authProperties.lockDuration(), now);
            userRepository.save(user);
            log.atWarn().addKeyValue("userId", user.getId()).log("Login failed: bad credentials");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.hasLoginFailureState()) {
            user.resetLoginFailures();
            user = userRepository.save(user);
        }
        log.atInfo().addKeyValue("userId", user.getId()).log("Login succeeded");
        return new AuthResult(jwtService.generateAccessToken(user), refreshTokenService.startSession(user.getId()));
    }

    /**
     * Exchanges a valid refresh token for a new access token + a rotated refresh
     * token.
     */
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedSession session = refreshTokenService.rotate(rawRefreshToken);
        return new AuthResult(jwtService.generateAccessToken(session.user()), session.refreshToken());
    }

    /**
     * Ends the session the token belongs to. Never fails for unknown tokens
     * (idempotent).
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeSession(rawRefreshToken);
    }

    public void logoutEverywhere(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
