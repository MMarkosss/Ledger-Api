package mmarkoss.ledger_api.features.statements.dto;

import java.util.List;

public record StatementPageRes(
        String order,
        List<StatementEntryRes> entries,
        Long nextCursor,
        boolean hasMore
) {
}
