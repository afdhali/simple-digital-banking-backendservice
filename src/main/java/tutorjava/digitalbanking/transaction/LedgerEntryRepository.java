package tutorjava.digitalbanking.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tutorjava.digitalbanking.transaction.dto.AccountMutationResponse;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * Account statement. Projects straight into a DTO (no entity hydration, no N+1) and is served by
     * idx_ledger_account_created (account_id, created_at DESC, id DESC).
     */
    @Query(value = """
            select new tutorjava.digitalbanking.transaction.dto.AccountMutationResponse(
                t.referenceNo, t.type, e.direction, e.amount, e.balanceAfter, t.description, e.createdAt)
            from LedgerEntry e join e.transaction t
            where e.accountId = :accountId
            order by e.createdAt desc, e.id desc
            """,
            countQuery = "select count(e) from LedgerEntry e where e.accountId = :accountId")
    Page<AccountMutationResponse> findStatement(@Param("accountId") UUID accountId, Pageable pageable);
}
