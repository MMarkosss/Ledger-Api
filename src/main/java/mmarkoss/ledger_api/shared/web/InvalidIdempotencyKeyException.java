package mmarkoss.ledger_api.shared.web;

import org.springframework.http.HttpStatus;

public final class InvalidIdempotencyKeyException extends DomainException {
    public InvalidIdempotencyKeyException() {
        super(HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Header Idempotency-Key é obrigatório");
    }
}
