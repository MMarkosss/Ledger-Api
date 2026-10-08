package MMarkoss.ledger_api.features.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Account {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Setter
    @Column(nullable = false, name = "holder_name")
    private String holderName;

    @Setter
    @Column(nullable = false, unique = true)
    private String document;

    @Setter
    @Column(nullable = false)
    private BigDecimal balance;

    @Setter
    @Version
    @Column(nullable = false)
    private Integer version;

    // Construtor anotado com @Builder focado na criação inicial
    @Builder
    public Account(String holderName, String document, BigDecimal balance) {
        this.holderName = holderName;
        this.document = document;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }
}
