package MMarkoss.ledger_api.features.accounts;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountBalanceOperator {
    public void deposit(Account acc, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Valor deve ser positivo");
        acc.applyDelta(amount);
    }

    public void withdraw(Account acc, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Valor deve ser positivo");
        if (acc.getBalance().compareTo(amount) < 0)
            throw new IllegalArgumentException("Saldo insuficiente");
        acc.applyDelta(amount.negate());
    }
}

