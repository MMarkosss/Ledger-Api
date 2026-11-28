package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidStatementQueryException extends DomainException {

    public InvalidStatementQueryException(String message) {
        super(HttpStatus.BAD_REQUEST, "Consulta inválida", message);
    }
}
