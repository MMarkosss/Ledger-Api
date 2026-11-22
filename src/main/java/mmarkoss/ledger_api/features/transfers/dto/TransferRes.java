package mmarkoss.ledger_api.features.transfers.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRes(UUID transactionId,
                          BigDecimal sourceBalance,
                          BigDecimal destinationBalance) {
}
