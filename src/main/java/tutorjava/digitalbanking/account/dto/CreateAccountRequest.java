package tutorjava.digitalbanking.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @Schema(example = "Tabungan Utama") @NotBlank @Size(max = 50) String accountName) {
}
