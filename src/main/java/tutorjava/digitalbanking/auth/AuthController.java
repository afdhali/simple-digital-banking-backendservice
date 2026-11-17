package tutorjava.digitalbanking.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tutorjava.digitalbanking.auth.dto.AuthResponse;
import tutorjava.digitalbanking.auth.dto.LoginRequest;
import tutorjava.digitalbanking.auth.dto.RefreshRequest;
import tutorjava.digitalbanking.auth.dto.RegisterRequest;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.response.ApiError;
import tutorjava.digitalbanking.config.OpenApiConfig;
import tutorjava.digitalbanking.security.AuthenticatedUser;
import tutorjava.digitalbanking.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh and logout. Web: refresh token in an HttpOnly cookie. "
        + "Mobile (header X-Client-Type: mobile): refresh token in the JSON body.")
public class AuthController {

    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String CLIENT_TYPE_MOBILE = "mobile";

    private final AuthService authService;
    private final RefreshTokenCookies cookies;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer")
    @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login. Returns a short-lived access token and starts a refresh-token session", description = "Web (default): the refresh token is set as an HttpOnly cookie and omitted from the body. "
            + "Mobile: send `X-Client-Type: mobile` to receive `refreshToken` in the body.")
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "423", description = "User temporarily locked after repeated failures", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "Use 'mobile' to get the refresh token in the response body") @RequestHeader(name = CLIENT_TYPE_HEADER, required = false) String clientType) {
        boolean bodyDelivery = CLIENT_TYPE_MOBILE.equalsIgnoreCase(clientType);
        return tokenResponse(authService.login(request), bodyDelivery);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token (the refresh token is rotated)", description = "Web: call with no body; the browser sends the HttpOnly cookie. "
            + "Mobile: send `{\"refreshToken\": \"...\"}`. Every refresh token is single-use: "
            + "replaying an already used token revokes the whole session.")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired or already used", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Cookie request from an untrusted Origin", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest http) {
        String fromBody = bodyToken(body);
        boolean bodyDelivery = fromBody != null;
        String rawToken;
        if (bodyDelivery) {
            rawToken = fromBody;
        } else {
            cookies.requireTrustedOrigin(http); // cookie => CSRF defence
            rawToken = cookies.read(http)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        }
        return tokenResponse(authService.refresh(rawToken), bodyDelivery);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout: revoke the current session (idempotent)", description = "Web: no body, the cookie identifies the session and is cleared. "
            + "Mobile: send `{\"refreshToken\": \"...\"}`. Always returns 204. "
            + "The access token already issued stays valid until it expires (max 15 minutes).")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(@Valid @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest http) {
        String fromBody = bodyToken(body);
        if (fromBody != null) {
            authService.logout(fromBody);
            return ResponseEntity.noContent().build();
        }
        cookies.requireTrustedOrigin(http);
        cookies.read(http).ifPresent(authService::logout);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @Operation(summary = "Logout from every device (revokes all sessions of the caller)")
    @ApiResponse(responseCode = "204", description = "All sessions revoked")
    public ResponseEntity<Void> logoutAll(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        authService.logoutEverywhere(principal.id());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    // ------------------------------------------------------------------ helpers
    private ResponseEntity<AuthResponse> tokenResponse(AuthResult result, boolean bodyDelivery) {
        if (bodyDelivery) {
            return ResponseEntity.ok(AuthResponse.from(result.accessToken(), result.refreshToken().value()));
        }
        ResponseCookie cookie = cookies.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthResponse.from(result.accessToken(), null));
    }

    private String bodyToken(RefreshRequest body) {
        return body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken().trim()
                : null;
    }
}
