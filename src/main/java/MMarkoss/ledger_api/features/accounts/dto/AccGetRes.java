package MMarkoss.ledger_api.features.accounts.dto;

import MMarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccGetRes(
        UUID id,
        String holderName,
        BigDecimal balance
) {
    public AccGetRes(Account acc) {
        this(
            acc.getId(),
            acc.getHolderName(),
            acc.getBalance()
        );
    }
}
