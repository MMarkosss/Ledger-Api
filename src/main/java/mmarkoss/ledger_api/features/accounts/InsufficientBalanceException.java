package mmarkoss.ledger_api.features.accounts;

public final class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Saldo insuficiente");
    }
}
