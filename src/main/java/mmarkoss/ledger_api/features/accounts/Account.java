package mmarkoss.ledger_api.features.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Conta é o aggregate root do saldo. {@link #credit} e {@link #debit} são públicos porque
 * o use case de depósito (e futuras operações) orquestra conta e ledger em pacotes
 * diferentes. O pareamento com {@code ledger_entry} é responsabilidade do use case, não
 * uma invariante deste aggregate.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, name = "holder_name")
    private String holderName;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Account(String holderName, String document) {
        rename(holderName);
        this.document = document;
        this.balance = BigDecimal.ZERO;
    }

    public static Account open(String holderName, String document) {
        return new Account(holderName, document);
    }

    public void credit(BigDecimal amount) {
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        this.balance = this.balance.subtract(amount);
    }

    public void rename(String holderName) {
        if (holderName == null || holderName.isBlank()) {
            throw new InvalidHolderNameException("Nome não pode ser vazio");
        }

        String normalized = holderName.trim();
        if (normalized.length() < 3 || normalized.length() > 80) {
            throw new InvalidHolderNameException(
                    "Informe um Nome valido com no minimo 3 caracteres");
        }
        this.holderName = normalized;
    }

    public static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException();
        }
    }
}
