package tutorjava.digitalbanking.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Internal, transport-independent description of "what the user wants to do".
 * The controller maps each request DTO to a command; processors only know about commands.
 */
public record TransactionCommand(
        UUID userId,
        TransactionType type,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String description) {

    public TransactionCommand {
        // request validation already limits the scale to 2, so this never rounds
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        description = description == null || description.isBlank() ? null : description.trim();
    }

    public static TransactionCommand deposit(UUID userId, String accountNumber, BigDecimal amount, String description) {
        return new TransactionCommand(userId, TransactionType.DEPOSIT, null, accountNumber, amount, description);
    }

    public static TransactionCommand withdrawal(UUID userId, String accountNumber, BigDecimal amount, String description) {
        return new TransactionCommand(userId, TransactionType.WITHDRAWAL, accountNumber, null, amount, description);
    }

    public static TransactionCommand transfer(UUID userId, String source, String destination,
                                              BigDecimal amount, String description) {
        return new TransactionCommand(userId, TransactionType.TRANSFER, source, destination, amount, description);
    }

    /** Canonical string of everything that defines the request; hashed for idempotency comparison. */
    public String fingerprint() {
        return String.join("|", type.name(),
                String.valueOf(sourceAccountNumber),
                String.valueOf(destinationAccountNumber),
                amount.toPlainString(),
                String.valueOf(description));
    }
}
