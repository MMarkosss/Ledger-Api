package MMarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountUpdaterTest {

    private Account conta() {
        return Account.builder().holderName("Maria Silva").document("12345678901").build();
    }

    @Test
    @DisplayName("Rejeita holderName nulo")
    void deveRejeitarNomeNulo() {
        assertThatThrownBy(() -> AccountUpdater.account(conta()).holderName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejeita holderName em branco")
    void deveRejeitarNomeEmBranco() {
        assertThatThrownBy(() -> AccountUpdater.account(conta()).holderName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejeita build sem holderName")
    void deveRejeitarBuildSemNome() {
        assertThatThrownBy(() -> AccountUpdater.account(conta()).build())
                .isInstanceOf(IllegalStateException.class);
    }
}
