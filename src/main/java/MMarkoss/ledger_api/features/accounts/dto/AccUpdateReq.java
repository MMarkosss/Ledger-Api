package MMarkoss.ledger_api.features.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccUpdateReq(
        @NotBlank
        @Size(min = 3, max= 80, message = "Informe um Nome valido com no minimo 3 caracteres")
        String holderName
) {
}
