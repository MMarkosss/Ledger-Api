package mmarkoss.ledger_api.features.deposits.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRes(UUID transactionId, BigDecimal balance) {
}
