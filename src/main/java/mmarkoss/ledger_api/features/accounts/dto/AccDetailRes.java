package mmarkoss.ledger_api.features.accounts.dto;

import mmarkoss.ledger_api.features.ledger.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccDetailRes(
        UUID id,
        String holderName,
        String document,
        BigDecimal balance
) {
    public AccDetailRes(Account acc) {
        this(
                acc.getId(),
                acc.getHolderName(),
                acc.getDocument(),
                acc.getBalance()
        );
    }
}
