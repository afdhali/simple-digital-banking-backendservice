package tutorjava.digitalbanking.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Acquires row locks on accounts. Propagation.MANDATORY guarantees this is only
 * ever called
 * from inside a transaction (a lock outside a transaction would be released
 * immediately).
 */
@Service
@RequiredArgsConstructor
public class AccountLockService {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Account> lock(Set<String> accountNumbers) {
        Map<String, Account> locked = new LinkedHashMap<>();
        accountRepository.findAllForUpdate(accountNumbers).forEach(a -> locked.put(a.getAccountNumber(), a));
        if (!locked.keySet().containsAll(new HashSet<>(accountNumbers))) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return locked;
    }

    /**
     * Locks one account and verifies it belongs to the user (foreign accounts look
     * like "not found").
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Account lockOwned(String accountNumber, UUID userId) {
        Account account = lock(Set.of(accountNumber)).get(accountNumber);
        if (!account.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return account;
    }
}
