package mmarkoss.ledger_api.features.accounts.dto;

import jakarta.validation.constraints.*;

public record AccPostReq(
        @NotBlank
        @Size(min = 3, max= 80, message = "Informe um Nome valido com no minimo 3 caracteres")
        String holderName,
        @NotBlank
        @Size (min = 11, max = 11, message = "O documento deve ter 11 caracteres")
        String document
) {
}