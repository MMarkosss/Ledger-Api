package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.features.ledger.EntryDirection;
import mmarkoss.ledger_api.features.ledger.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record StatementEntry(Long entryId,
                      UUID transactionId,
                      TransactionType type,
                      EntryDirection direction,
                      BigDecimal amount,
                      BigDecimal balanceAfter,
                      UUID counterpartyId,
                      Instant createdAt) {
}
