package MMarkoss.ledger_api.features.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Setter (AccessLevel.PACKAGE)
    @Column(nullable = false, name = "holder_name")
    private String holderName;

    @Column(nullable = false, unique = true)
    private String document;

    @Setter (AccessLevel.PRIVATE)
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

    // Construtor anotado com @Builder focado na criação inicial
    @Builder
    public Account(String holderName, String document) {
        this.holderName = holderName;
        this.document = document;
        this.balance = BigDecimal.ZERO;
    }

    //Necessario para AccountBalanceOperator
    void applyDelta(BigDecimal delta) {
        this.balance = this.balance.add(delta);
    }
}
