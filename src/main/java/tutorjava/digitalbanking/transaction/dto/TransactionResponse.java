package tutorjava.digitalbanking.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        @Schema(example = "TRX20261116AB12CD34EF") String referenceNo,
        TransactionType type,
        BigDecimal amount,
        @Schema(example = "IDR") String currency,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String description,
        Instant createdAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getReferenceNo(), t.getType(), t.getAmount(), t.getCurrency(),
                t.getSourceAccountNumber(), t.getDestinationAccountNumber(), t.getDescription(), t.getCreatedAt());
    }
}
