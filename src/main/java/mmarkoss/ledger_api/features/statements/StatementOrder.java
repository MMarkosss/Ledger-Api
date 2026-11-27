package mmarkoss.ledger_api.features.statements;

import java.util.Locale;

enum StatementOrder {
    ASC("asc"),
    DESC("desc");

    private final String wireName;

    StatementOrder(String wireName) {
        this.wireName = wireName;
    }

    String wireName() {
        return wireName;
    }

    static StatementOrder from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (StatementOrder order : values()) {
            if (order.name().equals(normalized)) {
                return order;
            }
        }

        throw new InvalidStatementQueryException("order deve ser 'asc' ou 'desc'");
    }
}
