package MMarkoss.ledger_api.features.accounts.dto;

import MMarkoss.ledger_api.features.accounts.Account;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AccPatchRes(
        UUID id,
        String holderName,
        String document,
        BigDecimal balance,
        Set<String> updatedFields
) {
    public AccPatchRes(Account acc, Set<String> updatedFields) {
        this(
                acc.getId(),
                acc.getHolderName(),
                acc.getDocument(),
                acc.getBalance(),
                Set.copyOf(updatedFields)
        );
    }
}
