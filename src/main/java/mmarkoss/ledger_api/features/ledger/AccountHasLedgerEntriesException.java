package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class AccountHasLedgerEntriesException extends DomainException {
    public AccountHasLedgerEntriesException(UUID id) {
        super(HttpStatus.CONFLICT,
                "Conta com lançamentos",
                "Conta id: " + id + " possui lançamentos e não pode ser excluída");
    }
}
