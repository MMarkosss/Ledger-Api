package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException() {
        super(HttpStatus.CONFLICT,
                "Conflito de idempotência",
                "Idempotency-Key reutilizada com corpo diferente",
                "IDEMPOTENCY_CONFLICT",
                false);
    }
}
