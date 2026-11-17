package tutorjava.digitalbanking.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tutorjava.digitalbanking.transaction.EntryDirection;
import tutorjava.digitalbanking.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of an account statement. Also used as a JPQL constructor projection. */
public record AccountMutationResponse(
        String referenceNo,
        TransactionType type,
        @Schema(description = "DEBIT = money out, CREDIT = money in") EntryDirection direction,
        BigDecimal amount,
        @Schema(description = "Account balance right after this entry") BigDecimal balanceAfter,
        String description,
        Instant createdAt) {
}
