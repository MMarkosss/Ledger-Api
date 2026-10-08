package MMarkoss.ledger_api.features.accounts.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import java.math.BigDecimal;

public record AccPostReqDTO(
        @NotBlank
        @Size (min = 3, message = "Informe um nome valido com no minimo 3 caracteres")
        String holderName,
        @Size (min = 11, max = 11, message = "O documento deve ter 11 caracteres")
        String documents,
        @NotNull
        @PositiveOrZero (message = "O saldo inicial não pode ser nulo ou negativo")
        BigDecimal balance
) {
}
