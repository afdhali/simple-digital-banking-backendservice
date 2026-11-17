package tutorjava.digitalbanking.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "app.banking")
public record BankingProperties(
        @NotBlank String currency,
        @NotNull @DecimalMin("0.01") BigDecimal maxTransactionAmount,
        @Min(1) int maxAccountsPerUser) {
}
