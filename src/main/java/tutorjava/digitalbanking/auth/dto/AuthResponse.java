package tutorjava.digitalbanking.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import tutorjava.digitalbanking.security.IssuedToken;

public record AuthResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(description = "Only returned to mobile clients (header X-Client-Type: mobile). "
                + "Browsers receive it as an HttpOnly cookie instead.") String refreshToken) {

    public static AuthResponse from(IssuedToken token, String refreshToken) {
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), refreshToken);
    }
}
