package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.shared.web.DomainException;
import org.springframework.http.HttpStatus;

public final class SameAccountTransferException extends DomainException {
    public SameAccountTransferException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT,
                "Transferência inválida",
                "Conta de origem e destino devem ser diferentes");
    }
}
