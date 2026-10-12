package MMarkoss.ledger_api.features.accounts.dto;

import MMarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;

public record AccGetRes(
        String holderName,
        BigDecimal balance
) {
    public AccGetRes(Account acc) {
        this(
            acc.getHolderName(),
            acc.getBalance()
        );
    }
}
