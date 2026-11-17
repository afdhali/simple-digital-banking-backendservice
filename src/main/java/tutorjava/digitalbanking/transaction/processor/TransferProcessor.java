package tutorjava.digitalbanking.transaction.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.account.AccountLockService;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.transaction.Transaction;
import tutorjava.digitalbanking.transaction.TransactionCommand;
import tutorjava.digitalbanking.transaction.TransactionType;

import java.util.Map;
import java.util.Set;

/** Moves money from one of the caller's accounts to any other account, atomically. */
@Component
@RequiredArgsConstructor
public class TransferProcessor implements TransactionProcessor {

    private final AccountLockService accountLockService;
    private final TransactionRecorder recorder;

    @Override
    public TransactionType supportedType() {
        return TransactionType.TRANSFER;
    }

    @Override
    public Transaction process(TransactionCommand command) {
        String sourceNumber = command.sourceAccountNumber();
        String destinationNumber = command.destinationAccountNumber();
        if (sourceNumber.equals(destinationNumber)) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        // Both rows are locked in one statement, in a deterministic order -> no deadlocks.
        Map<String, Account> locked = accountLockService.lock(Set.of(sourceNumber, destinationNumber));
        Account source = locked.get(sourceNumber);
        Account destination = locked.get(destinationNumber);

        if (!source.isOwnedBy(command.userId())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new BusinessException(ErrorCode.CURRENCY_MISMATCH);
        }

        source.debit(command.amount());
        destination.credit(command.amount());
        return recorder.record(command, source, destination);
    }
}
