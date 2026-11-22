package mmarkoss.ledger_api.features.transfers;

public final class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Conta de origem e destino devem ser diferentes");
    }
}
