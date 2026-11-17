package tutorjava.digitalbanking.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** A transaction is visible only to the owner of its source or destination account. */
    @Query("""
            select t from Transaction t
            where t.referenceNo = :referenceNo
              and (t.sourceAccountNumber in (select a.accountNumber from Account a where a.userId = :userId)
                or t.destinationAccountNumber in (select a.accountNumber from Account a where a.userId = :userId))
            """)
    Optional<Transaction> findVisibleToUser(@Param("referenceNo") String referenceNo,
                                            @Param("userId") UUID userId);
}
