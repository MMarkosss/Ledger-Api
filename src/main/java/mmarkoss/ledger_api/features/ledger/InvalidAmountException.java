package mmarkoss.ledger_api.features.ledger;

public final class InvalidAmountException extends RuntimeException {
    public InvalidAmountException() {
        super("Valor deve ser positivo");
    }
}
