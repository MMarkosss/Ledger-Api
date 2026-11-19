package mmarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account acc;

    @BeforeEach
    void setUp() {
        this.acc = Account.open("Maria Silva", "12345678901");
    }

    @Test
    @DisplayName("Deve aceitar deposito pelo credit()")
    void deveAceitarDeposito() {
        acc.credit(new BigDecimal("100"));
        assertThat(acc.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Deve aceitar saque pelo debit()")
    void deveAceitarSaque() {
        acc.credit(new BigDecimal("100"));
        acc.debit(new BigDecimal("30"));
        assertThat(acc.getBalance()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("Deve rejeitar saque sem saldo")
    void deveRejeitarSaqueSemSaldo() {
        acc.credit(new BigDecimal("100"));

        assertThatThrownBy(() -> acc.debit(new BigDecimal("1000")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Saldo insuficiente")
                .hasNoCause();
    }

    @Test
    @DisplayName("Deve rejeitar deposito com valor negativo")
    void deveRejeitarDepositoNegativo() {
        assertThatThrownBy(() -> acc.credit(new BigDecimal("-1")))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Valor deve ser positivo")
                .hasNoCause();
    }

    @Test
    @DisplayName("Deve rejeitar deposito com valor nulo")
    void deveRejeitarDepositoNulo() {
        assertThatThrownBy(() -> acc.credit(null))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessage("Valor deve ser positivo")
                .hasNoCause();
    }

    @Test
    @DisplayName("Rejeita holderName nulo")
    void deveRejeitarNomeNulo() {
        assertThatThrownBy(() -> acc.rename(null))
                .isInstanceOf(InvalidHolderNameException.class);
    }

    @Test
    @DisplayName("Rejeita holderName em branco")
    void deveRejeitarNomeEmBranco() {
        assertThatThrownBy(() -> acc.rename("   "))
                .isInstanceOf(InvalidHolderNameException.class);
    }

    @Test
    @DisplayName("Normaliza holderName e aplica o limite de 3 a 80")
    void deveNormalizarNomeEValidarTamanho() {
        acc.rename("  Maria Silva  ");
        assertThat(acc.getHolderName()).isEqualTo("Maria Silva");

        assertThatThrownBy(() -> acc.rename("Ab"))
                .isInstanceOf(InvalidHolderNameException.class);
        assertThatThrownBy(() -> acc.rename("a".repeat(81)))
                .isInstanceOf(InvalidHolderNameException.class);
    }
}
