package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class DuplicateDocumentException extends DomainException {
    public DuplicateDocumentException() {
        super(HttpStatus.CONFLICT, "Conflito", "Documento já cadastrado");
    }
}
