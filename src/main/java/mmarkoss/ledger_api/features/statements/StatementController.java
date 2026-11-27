package mmarkoss.ledger_api.features.statements;

import lombok.AllArgsConstructor;
import mmarkoss.ledger_api.features.statements.dto.StatementEntryRes;
import mmarkoss.ledger_api.features.statements.dto.StatementPageRes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/accounts/{accountId}/statements")
public class StatementController {

    private final StatementService statementService;

    @GetMapping
    public ResponseEntity<StatementPageRes> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "desc") String order) {

        StatementQuery query = StatementQuery.of(limit, cursor, order);
        StatementPage page = statementService.getStatement(accountId, query);

        StatementPageRes response = new StatementPageRes(
                page.order().wireName(),
                page.entries().stream().map(this::toRes).toList(),
                page.nextCursor(),
                page.hasMore());

        return ResponseEntity.ok(response);
    }

    private StatementEntryRes toRes(StatementEntry entry) {
        return new StatementEntryRes(
                entry.entryId(),
                entry.transactionId(),
                entry.type(),
                entry.direction(),
                entry.amount(),
                entry.balanceAfter(),
                entry.counterpartyId(),
                entry.createdAt());
    }
}
