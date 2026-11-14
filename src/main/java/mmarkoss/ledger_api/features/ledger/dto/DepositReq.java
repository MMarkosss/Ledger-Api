package mmarkoss.ledger_api.features.ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositReq(
        @NotNull(message = "Informe o valor")
        BigDecimal amount
) {
}
