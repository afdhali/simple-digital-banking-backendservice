package tutorjava.digitalbanking.transaction.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.account.AccountLockService;
import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionCommand;
import tutorjava.digitalbanking.transaction.TransactionType;

/** Cash-out from one of the caller's own accounts. */
@Component
@RequiredArgsConstructor
public class WithdrawalProcessor implements TransactionProcessor {

    private final AccountLockService accountLockService;
    private final TransactionRecorder recorder;

    @Override
    public TransactionType supportedType() {
        return TransactionType.WITHDRAWAL;
    }

    @Override
    public Transaction process(TransactionCommand command) {
        Account account = accountLockService.lockOwned(command.sourceAccountNumber(), command.userId());
        account.debit(command.amount());
        return recorder.record(command, account, null);
    }
}
