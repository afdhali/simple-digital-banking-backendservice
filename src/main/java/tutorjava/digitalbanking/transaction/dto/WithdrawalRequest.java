package tutorjava.digitalbanking.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @Schema(example = "1234567890", description = "One of your own accounts")
        @NotBlank @Pattern(regexp = "^\\d{10}$", message = "must be a 10-digit account number")
        String accountNumber,

        @Schema(example = "100000.00")
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2)
        BigDecimal amount,

        @Schema(example = "ATM cash withdrawal")
        @Size(max = 140)
        String description) {
}
