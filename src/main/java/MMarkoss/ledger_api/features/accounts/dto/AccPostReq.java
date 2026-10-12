package MMarkoss.ledger_api.features.accounts.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AccPostReq(
        @NotBlank
        @Size (min = 3, message = "Informe um holderName valido com no minimo 3 caracteres")
        String holderName,
        @Size (min = 11, max = 11, message = "O documento deve ter 11 caracteres")
        String document,
        @NotNull
        @PositiveOrZero (message = "O saldo inicial não pode ser nulo ou negativo")
        BigDecimal balance
) {
}
