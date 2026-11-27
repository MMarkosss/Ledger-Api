package mmarkoss.ledger_api.features.statements;

record StatementQuery(int limit, Long cursor, StatementOrder order) {

    static final int MAX_LIMIT = 100;

    static StatementQuery of(int limit, Long cursor, String order) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidStatementQueryException(
                    "limit deve estar entre 1 e " + MAX_LIMIT);
        }
        if (cursor != null && cursor <= 0) {
            throw new InvalidStatementQueryException("cursor deve ser positivo");
        }

        return new StatementQuery(limit, cursor, StatementOrder.from(order));
    }
}
