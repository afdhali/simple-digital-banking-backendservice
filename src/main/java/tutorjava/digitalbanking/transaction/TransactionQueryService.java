package tutorjava.digitalbanking.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.account.Account;
import tutorjava.digitalbanking.account.AccountRepository;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.response.PageResponse;
import tutorjava.digitalbanking.transaction.dto.AccountMutationResponse;
import tutorjava.digitalbanking.transaction.dto.TransactionResponse;

import java.util.UUID;

/** Read side (CQRS-lite): read-only transactions, no locks, DTO projections. */
@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(UUID userId, String referenceNo) {
        return transactionRepository.findVisibleToUser(referenceNo, userId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountMutationResponse> getStatement(UUID userId, String accountNumber,
                                                               int page, int size) {
        Account account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        // never trust paging input: clamp it so nobody can request a million rows
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageResponse.from(
                ledgerEntryRepository.findStatement(account.getId(), PageRequest.of(safePage, safeSize)));
    }
}
