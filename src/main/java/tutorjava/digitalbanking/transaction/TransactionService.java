package tutorjava.digitalbanking.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.config.BankingProperties;
import tutorjava.digitalbanking.idempotency.IdempotencyClaim;
import tutorjava.digitalbanking.idempotency.IdempotencyService;
import tutorjava.digitalbanking.transaction.dto.TransactionResponse;
import tutorjava.digitalbanking.transaction.processor.TransactionProcessorRegistry;

/**
 * FACADE / orchestrator for every money movement.
 *
 * ONE @Transactional boundary wraps: idempotency claim -> account row locks -> balance changes ->
 * transaction + ledger rows -> idempotency completion. Any failure rolls back ALL of it (atomicity),
 * balances are protected by row locks (isolation) and CHECK constraints (consistency).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final IdempotencyService idempotencyService;
    private final TransactionProcessorRegistry processorRegistry;
    private final TransactionRepository transactionRepository;
    private final BankingProperties bankingProperties;

    @Transactional
    public TransactionResult execute(String idempotencyKey, TransactionCommand command) {
        if (command.amount().compareTo(bankingProperties.maxTransactionAmount()) > 0) {
            throw new BusinessException(ErrorCode.AMOUNT_LIMIT_EXCEEDED);
        }

        IdempotencyClaim claim = idempotencyService.claim(command.userId(), idempotencyKey, command.fingerprint());

        return switch (claim) {
            case IdempotencyClaim.Fresh fresh -> {
                Transaction transaction = processorRegistry.get(command.type()).process(command);
                // idempotency_keys.transaction_id is a FK: the transaction row must physically exist
                // (and balance updates are sent to the DB) before the bulk UPDATE below runs.
                transactionRepository.flush();
                idempotencyService.complete(fresh.claimId(), transaction.getId());
                log.atInfo()
                        .addKeyValue("referenceNo", transaction.getReferenceNo())
                        .addKeyValue("type", transaction.getType())
                        .addKeyValue("amount", transaction.getAmount())
                        .addKeyValue("userId", command.userId())
                        .log("Transaction completed");
                yield new TransactionResult(TransactionResponse.from(transaction), false);
            }
            case IdempotencyClaim.Replay replay -> {
                Transaction transaction = transactionRepository.findById(replay.transactionId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
                yield new TransactionResult(TransactionResponse.from(transaction), true);
            }
        };
    }
}
