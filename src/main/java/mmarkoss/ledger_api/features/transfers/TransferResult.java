package mmarkoss.ledger_api.features.transfers;

import java.math.BigDecimal;
import java.util.UUID;

record TransferResult(UUID transactionId,
                      BigDecimal sourceBalance,
                      BigDecimal destinationBalance,
                      boolean replayed) {
}
