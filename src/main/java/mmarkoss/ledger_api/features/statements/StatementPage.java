package mmarkoss.ledger_api.features.statements;

import java.util.List;

record StatementPage(StatementOrder order,
                     List<StatementEntry> entries,
                     Long nextCursor,
                     boolean hasMore) {
}
