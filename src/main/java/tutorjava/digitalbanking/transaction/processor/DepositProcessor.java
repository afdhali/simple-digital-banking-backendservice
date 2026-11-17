package tutorjava.digitalbanking.transaction.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.account.AccountLockService;
import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionCommand;
import tutorjava.digitalbanking.transaction.TransactionType;

/** Cash-in to one of the caller's own accounts (simulates a top-up from an external channel). */
@Component
@RequiredArgsConstructor
public class DepositProcessor implements TransactionProcessor {

    private final AccountLockService accountLockService;
    private final TransactionRecorder recorder;

    @Override
    public TransactionType supportedType() {
        return TransactionType.DEPOSIT;
    }

    @Override
    public Transaction process(TransactionCommand command) {
        Account account = accountLockService.lockOwned(command.destinationAccountNumber(), command.userId());
        account.credit(command.amount());
        return recorder.record(command, null, account);
    }
}
