package tutorjava.digitalbanking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Mobile clients send the refresh token in the body. Browsers send nothing: the
 * HttpOnly cookie is used.
 */
public record RefreshRequest(
        @Schema(description = "Mobile only. Omit for browsers (the HttpOnly cookie is used instead).") @Size(max = 256) String refreshToken) {
}
