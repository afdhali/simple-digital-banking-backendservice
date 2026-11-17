package tutorjava.digitalbanking.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Account> findByAccountNumberAndUserId(String accountNumber, UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    long countByUserId(UUID userId);

    /**
     * SELECT ... FOR UPDATE. Rows are always locked in the same order
     * (account_number ASC), so two
     * concurrent transfers A->B and B->A cannot deadlock. Must run inside a
     * transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber in :numbers order by a.accountNumber")
    List<Account> findAllForUpdate(@Param("numbers") Collection<String> numbers);
}
