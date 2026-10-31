package MMarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import java.math.BigDecimal;


class AccountBalanceOperatorTest {

    private Account acc;
    private AccountBalanceOperator operator;

    @BeforeEach
    void setUp () {
        this.acc = Account.builder().holderName("Maria Silva").document("12345678901").build();
        this.operator = new AccountBalanceOperator();
    }

    @Test
    @DisplayName("Deve aceitar deposito pelo deposit()")
    void deveAceitarDeposito() {
        operator.deposit(acc, new BigDecimal("100"));
        assertThat(acc.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Deve aceitar saque pelo withdraw()")
    void deveAceitarWithdraw() {
        operator.deposit(acc, new BigDecimal("100"));
        operator.withdraw(acc,new BigDecimal("30"));
        assertThat(acc.getBalance()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("Deve rejeitar saque sem saldo")
    void naoDeveAceitarWithdrawSemSaldo () {
        operator.deposit(acc, new BigDecimal("100"));
        assertThatThrownBy(()->operator.withdraw(acc,new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Saldo insuficiente")
                .hasNoCause();
    }

    @Test
    @DisplayName("Deve rejeitar deposito com valor negativo")
    void deveRejeitarDepositoNegativo() {
        operator.deposit(acc, new BigDecimal("100"));
        assertThatThrownBy(() -> operator.deposit(acc, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valor deve ser positivo")
                .hasNoCause();
    }

    @Test
    @DisplayName("Deve rejeitar deposito com valor nulo")
    void deveRejeitarDepositoNulo() {
        operator.deposit(acc, new BigDecimal("100"));
        assertThatThrownBy(() -> operator.deposit(acc, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valor deve ser positivo")
                .hasNoCause();
    }
}
