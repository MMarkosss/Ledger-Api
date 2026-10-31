package MMarkoss.ledger_api.features.accounts.dto;

import MMarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccGetOneRes(
        UUID id,
        String holderName,
        String document,
        BigDecimal balance
) {
    public AccGetOneRes (Account acc) {
        this(
        acc.getId(),
        acc.getHolderName(),
        acc.getDocument(),
        acc.getBalance()
        );
    }
}
