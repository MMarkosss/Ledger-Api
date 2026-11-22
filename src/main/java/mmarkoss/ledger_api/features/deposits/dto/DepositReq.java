package mmarkoss.ledger_api.features.deposits.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositReq(
        @NotNull(message = "Informe o valor")
        @Digits(integer = 19, fraction = 4)
        BigDecimal amount
) {
}
