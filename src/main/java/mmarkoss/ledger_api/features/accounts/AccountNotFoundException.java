package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND,
                "Conta não encontrada",
                "Conta id: " + id + " não foi encontrada");
    }
}
