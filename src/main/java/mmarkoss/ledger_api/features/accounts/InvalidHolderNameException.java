package mmarkoss.ledger_api.features.accounts;

public final class InvalidHolderNameException extends RuntimeException {
    public InvalidHolderNameException(String message) {
        super(message);
    }
}
