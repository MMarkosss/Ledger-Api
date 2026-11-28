package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class InvalidHolderNameException extends DomainException {
    public InvalidHolderNameException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Nome inválido", message);
    }
}
