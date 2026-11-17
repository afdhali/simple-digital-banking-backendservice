package tutorjava.digitalbanking.transaction.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.transaction.EntryDirection;
import tutorjava.digitalbanking.transaction.LedgerEntry;
import tutorjava.digitalbanking.transaction.LedgerEntryRepository;
import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionCommand;
import tutorjava.digitalbanking.transaction.TransactionRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared persistence step of every processor: writes the transaction header and one ledger entry per
 * affected account. Call it AFTER the account balances have been updated so balance_after is correct.
 */
@Component
@RequiredArgsConstructor
public class TransactionRecorder {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SecureRandom random = new SecureRandom();

    /** @param source null for deposits; @param destination null for withdrawals */
    public Transaction record(TransactionCommand command, Account source, Account destination) {
        String currency = source != null ? source.getCurrency() : destination.getCurrency();

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .referenceNo(nextReference())
                .type(command.type())
                .amount(command.amount())
                .currency(currency)
                .sourceAccountNumber(source != null ? source.getAccountNumber() : null)
                .destinationAccountNumber(destination != null ? destination.getAccountNumber() : null)
                .description(command.description())
                .createdBy(command.userId())
                .build());

        List<LedgerEntry> entries = new ArrayList<>(2);
        if (source != null) {
            entries.add(LedgerEntry.of(transaction, source.getId(), EntryDirection.DEBIT,
                    command.amount(), source.getBalance()));
        }
        if (destination != null) {
            entries.add(LedgerEntry.of(transaction, destination.getId(), EntryDirection.CREDIT,
                    command.amount(), destination.getBalance()));
        }
        ledgerEntryRepository.saveAll(entries);
        return transaction;
    }

    private String nextReference() {
        StringBuilder sb = new StringBuilder("TRX")
                .append(LocalDate.now(ZoneOffset.UTC).format(DATE));
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
