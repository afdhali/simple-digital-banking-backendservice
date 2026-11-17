package tutorjava.digitalbanking.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.account.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        @Schema(example = "1234567890") String accountNumber,
        @Schema(example = "Tabungan Utama") String accountName,
        @Schema(example = "IDR") String currency,
        @Schema(example = "1500000.00") BigDecimal balance,
        AccountStatus status,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getAccountNumber(), account.getAccountName(),
                account.getCurrency(), account.getBalance(), account.getStatus(), account.getCreatedAt());
    }
}
