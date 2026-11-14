package mmarkoss.ledger_api.features.deposits;

import java.math.BigDecimal;
import java.util.UUID;

record DepositResult(UUID transactionId, BigDecimal balance, boolean replayed) {
}
