package tutorjava.digitalbanking.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import tutorjava.digitalbanking.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/** Immutable financial fact (also enforced by a DB trigger). Corrections are new transactions. */
@Getter
@Immutable
@Entity
@Table(name = "transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction extends BaseEntity {

    @Column(name = "reference_no", nullable = false, updatable = false, length = 30)
    private String referenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "source_account_number", updatable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", updatable = false, length = 10)
    private String destinationAccountNumber;

    @Column(name = "description", updatable = false, length = 140)
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Builder
    private Transaction(String referenceNo, TransactionType type, BigDecimal amount, String currency,
                        String sourceAccountNumber, String destinationAccountNumber,
                        String description, UUID createdBy) {
        this.referenceNo = referenceNo;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.description = description;
        this.createdBy = createdBy;
    }
}
