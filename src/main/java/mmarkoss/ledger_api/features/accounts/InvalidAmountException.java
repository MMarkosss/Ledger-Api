package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class InvalidAmountException extends DomainException {
    public InvalidAmountException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Valor inválido", "Valor deve ser positivo");
    }
}
