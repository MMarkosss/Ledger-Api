package mmarkoss.ledger_api.features.accounts;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID id) {
        super("Conta id: " + id + " não foi encontrada");
    }
}
