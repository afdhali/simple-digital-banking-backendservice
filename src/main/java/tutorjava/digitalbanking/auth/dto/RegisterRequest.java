package tutorjava.digitalbanking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "budi@example.com") @NotBlank @Email @Size(max = 254) String email,

        @Schema(example = "Budi Santoso") @NotBlank @Size(max = 100) String fullName,

        @Schema(example = "Str0ngPassw0rd!", description = "8-72 printable ASCII chars, at least one letter and one digit") @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,72}$", message = "must be 8-72 printable ASCII characters (no spaces) with at least one letter and one digit") String password) {
}
