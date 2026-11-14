package mmarkoss.ledger_api.features.accounts;

public final class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException() {
        super("Documento já cadastrado");
    }
}
