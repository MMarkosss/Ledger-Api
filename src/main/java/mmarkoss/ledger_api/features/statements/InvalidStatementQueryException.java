package mmarkoss.ledger_api.features.statements;

public class InvalidStatementQueryException extends RuntimeException {

    public InvalidStatementQueryException(String message) {
        super(message);
    }
}
