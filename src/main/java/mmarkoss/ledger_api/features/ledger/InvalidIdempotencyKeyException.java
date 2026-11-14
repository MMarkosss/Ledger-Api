package mmarkoss.ledger_api.features.ledger;

public final class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Header Idempotency-Key é obrigatório");
    }
}
