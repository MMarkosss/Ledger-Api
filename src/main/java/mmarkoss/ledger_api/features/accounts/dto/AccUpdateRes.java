package mmarkoss.ledger_api.features.accounts.dto;

import mmarkoss.ledger_api.features.ledger.Account;

import java.util.UUID;

public record AccUpdateRes(
        UUID id,
        String holderName
) {
    public AccUpdateRes (Account acc) {
        this(
                acc.getId(),
                acc.getHolderName()
        );
    }
}
