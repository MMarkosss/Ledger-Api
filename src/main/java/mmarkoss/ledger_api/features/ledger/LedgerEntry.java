package mmarkoss.ledger_api.features.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Convert(converter = EntryDirectionConverter.class)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, columnDefinition = "char(1)")
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_id")
    private UUID counterpartyId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public LedgerEntry(UUID transactionId,
                       UUID accountId,
                       EntryDirection direction,
                       BigDecimal amount,
                       BigDecimal balanceAfter,
                       UUID counterpartyId) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.counterpartyId = counterpartyId;
    }
}
