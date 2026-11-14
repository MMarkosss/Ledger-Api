package mmarkoss.ledger_api.features.ledger;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency-Key reutilizada com corpo diferente");
    }
}
