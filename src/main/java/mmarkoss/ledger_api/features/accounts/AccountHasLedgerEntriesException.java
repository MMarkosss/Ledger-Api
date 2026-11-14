package mmarkoss.ledger_api.features.accounts;

import java.util.UUID;

public final class AccountHasLedgerEntriesException extends RuntimeException {
    public AccountHasLedgerEntriesException(UUID id) {
        super("Conta id: " + id + " possui lançamentos e não pode ser excluída");
    }
}
