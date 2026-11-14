package mmarkoss.ledger_api.features.ledger;

import java.math.BigDecimal;
import java.util.UUID;

record DepositResult(UUID transactionId, BigDecimal balance, boolean replayed) {
}
