package MMarkoss.ledger_api.features.accounts.dto;

import MMarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;

public record AccGetOneRes(
        String holderName,
        String document,
        BigDecimal balance
) {
    public AccGetOneRes (Account acc) {
        this(
        acc.getHolderName(),
        acc.getDocument(),
        acc.getBalance()
        );
    }
}
