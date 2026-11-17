package tutorjava.digitalbanking.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.account.dto.AccountResponse;
import tutorjava.digitalbanking.account.dto.CreateAccountRequest;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.config.BankingProperties;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int MAX_NUMBER_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator numberGenerator;
    private final BankingProperties bankingProperties;

    @Transactional
    public AccountResponse open(UUID userId, CreateAccountRequest request) {
        if (accountRepository.countByUserId(userId) >= bankingProperties.maxAccountsPerUser()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LIMIT_REACHED);
        }
        Account account = Account.open(userId, uniqueAccountNumber(), request.accountName().trim(),
                bankingProperties.currency());
        accountRepository.save(account);
        log.atInfo()
                .addKeyValue("userId", userId)
                .addKeyValue("accountNumber", account.getAccountNumber())
                .log("Account opened");
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listOwn(UUID userId) {
        return accountRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getOwn(UUID userId, String accountNumber) {
        return accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private String uniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            String candidate = numberGenerator.next();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate; // the UNIQUE constraint still guards against a race
            }
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }
}
