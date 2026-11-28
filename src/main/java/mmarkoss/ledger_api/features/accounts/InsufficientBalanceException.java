package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Saldo insuficiente", "Saldo insuficiente");
    }
}
