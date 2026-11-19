package mmarkoss.ledger_api.shared.web;

public final class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Header Idempotency-Key é obrigatório");
    }
}
