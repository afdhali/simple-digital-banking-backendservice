package tutorjava.digitalbanking.transaction.processor;

import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionCommand;
import tutorjava.digitalbanking.transaction.TransactionType;

/**
 * STRATEGY: one implementation per transaction type. Adding a new type (e.g. bill payment)
 * means adding one class, without touching TransactionService (open/closed principle).
 * Implementations run inside the caller's database transaction.
 */
public interface TransactionProcessor {

    TransactionType supportedType();

    Transaction process(TransactionCommand command);
}
