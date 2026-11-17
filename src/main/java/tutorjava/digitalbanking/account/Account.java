package tutorjava.digitalbanking.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tutorjava.digitalbanking.common.entity.AuditableEntity;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rich domain object: balance can only change through credit()/debit(), which
 * enforce the
 * business invariants (active account, no overdraft). The DB CHECK (balance >=
 * 0) is the safety net.
 */
@Getter
@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends AuditableEntity {

    @Column(name = "account_number", nullable = false, updatable = false, length = 10)
    private String accountNumber;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "account_name", nullable = false, length = 50)
    private String accountName;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    public static Account open(UUID userId, String accountNumber, String accountName, String currency) {
        Account account = new Account();
        account.userId = userId;
        account.accountNumber = accountNumber;
        account.accountName = accountName;
        account.currency = currency;
        account.balance = BigDecimal.ZERO.setScale(2);
        account.status = AccountStatus.ACTIVE;
        return account;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public void credit(BigDecimal amount) {
        requireActive();
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requireActive();
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        balance = balance.subtract(amount);
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }
}
