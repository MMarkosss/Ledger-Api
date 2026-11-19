package mmarkoss.ledger_api.features.accounts.dto;

import mmarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccSummaryRes(
        UUID id,
        String holderName,
        BigDecimal balance
) {
    public AccSummaryRes(Account acc) {
        this(
            acc.getId(),
            acc.getHolderName(),
            acc.getBalance()
        );
    }
}
