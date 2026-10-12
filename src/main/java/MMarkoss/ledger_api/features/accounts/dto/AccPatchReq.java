package MMarkoss.ledger_api.features.accounts.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccPatchReq(
        @Size(min=3, max=100, message = "Nome deve ter pelo menos 3 caracteres")
        String holderName,
        String document,
        @PositiveOrZero BigDecimal balance
        ){}

