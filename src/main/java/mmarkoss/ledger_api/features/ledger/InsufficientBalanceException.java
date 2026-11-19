package mmarkoss.ledger_api.features.ledger;

public final class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Saldo insuficiente");
    }
}
