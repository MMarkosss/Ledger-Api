package mmarkoss.ledger_api.features.accounts;

public final class InvalidAmountException extends RuntimeException {
    public InvalidAmountException() {
        super("Valor deve ser positivo");
    }
}
