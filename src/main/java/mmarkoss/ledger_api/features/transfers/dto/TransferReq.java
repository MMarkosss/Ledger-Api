package mmarkoss.ledger_api.features.transfers.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferReq(
        @NotNull(message = "Informe a conta de origem")
        UUID sourceAccountId,
        @NotNull(message = "Informe a conta de destino")
        UUID destinationAccountId,
        @NotNull(message = "Informe o valor")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount
) {
}
