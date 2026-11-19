package mmarkoss.ledger_api.features.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lançamento append-only do ledger. A criação passa pelas factories package-private
 * {@link #credit} e {@link #debit}, usadas pelo {@link LedgerPostingService}; não há builder
 * público nem setters. O posting service pareia a criação do lançamento com
 * {@code Account.credit}/{@code Account.debit}.
 */
@Entity
@Table(name = "ledger_entry")
@Getter
@Immutable
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

    private LedgerEntry(UUID transactionId,
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

    static LedgerEntry credit(UUID transactionId,
                              UUID accountId,
                              BigDecimal amount,
                              BigDecimal balanceAfter) {
        return credit(transactionId, accountId, amount, balanceAfter, null);
    }

    static LedgerEntry credit(UUID transactionId,
                              UUID accountId,
                              BigDecimal amount,
                              BigDecimal balanceAfter,
                              UUID counterpartyId) {
        return create(EntryDirection.CREDIT, transactionId, accountId, amount, balanceAfter,
                counterpartyId);
    }

    static LedgerEntry debit(UUID transactionId,
                             UUID accountId,
                             BigDecimal amount,
                             BigDecimal balanceAfter) {
        return debit(transactionId, accountId, amount, balanceAfter, null);
    }

    static LedgerEntry debit(UUID transactionId,
                             UUID accountId,
                             BigDecimal amount,
                             BigDecimal balanceAfter,
                             UUID counterpartyId) {
        return create(EntryDirection.DEBIT, transactionId, accountId, amount, balanceAfter,
                counterpartyId);
    }

    private static LedgerEntry create(EntryDirection direction,
                                      UUID transactionId,
                                      UUID accountId,
                                      BigDecimal amount,
                                      BigDecimal balanceAfter,
                                      UUID counterpartyId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("transactionId não pode ser nulo");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("accountId não pode ser nulo");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount deve ser positivo");
        }
        if (balanceAfter == null || balanceAfter.signum() < 0) {
            throw new IllegalArgumentException("balanceAfter não pode ser negativo");
        }

        return new LedgerEntry(transactionId, accountId, direction, amount, balanceAfter,
                counterpartyId);
    }
}
