package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.features.statements.dto.StatementEntryRes;
import mmarkoss.ledger_api.features.statements.dto.StatementPageRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatementPaginationIT extends StatementITSupport {

    @Test
    @DisplayName("Cursor pagina desc e asc sem sobrepor lançamentos")
    void devePaginarPorCursor() {
        UUID account = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        depositar(account, "dep-1", "10.00");
        depositar(account, "dep-2", "20.00");
        depositar(account, "dep-3", "30.00");

        StatementPageRes descFirst = extratoOk(account, "?limit=2&order=desc");

        assertThat(descFirst.order()).isEqualTo("desc");
        assertThat(descFirst.entries()).hasSize(2);
        assertThat(descFirst.entries().get(0).amount()).isEqualByComparingTo("30");
        assertThat(descFirst.entries().get(1).amount()).isEqualByComparingTo("20");
        assertThat(descFirst.hasMore()).isTrue();
        assertThat(descFirst.nextCursor())
                .isEqualTo(descFirst.entries().getLast().entryId());

        StatementPageRes descSecond = extratoOk(account,
                "?limit=2&order=desc&cursor=" + descFirst.nextCursor());

        assertThat(descSecond.entries()).hasSize(1);
        assertThat(descSecond.entries().getFirst().amount()).isEqualByComparingTo("10");
        assertThat(descSecond.hasMore()).isFalse();
        assertThat(descSecond.nextCursor()).isNull();
        assertThat(descSecond.entries().getFirst().entryId())
                .isLessThan(descFirst.entries().getLast().entryId());

        StatementPageRes ascFirst = extratoOk(account, "?limit=2&order=asc");

        assertThat(ascFirst.order()).isEqualTo("asc");
        assertThat(ascFirst.entries()).hasSize(2);
        assertThat(ascFirst.entries().get(0).amount()).isEqualByComparingTo("10");
        assertThat(ascFirst.entries().get(1).amount()).isEqualByComparingTo("20");
        assertThat(ascFirst.hasMore()).isTrue();
        assertThat(ascFirst.nextCursor())
                .isEqualTo(ascFirst.entries().getLast().entryId());

        StatementPageRes ascSecond = extratoOk(account,
                "?limit=2&order=asc&cursor=" + ascFirst.nextCursor());

        assertThat(ascSecond.entries()).hasSize(1);
        assertThat(ascSecond.entries().getFirst().amount()).isEqualByComparingTo("30");
        assertThat(ascSecond.hasMore()).isFalse();
        assertThat(ascSecond.nextCursor()).isNull();
        assertThat(ascSecond.entries().getFirst().entryId())
                .isGreaterThan(ascFirst.entries().getLast().entryId());

        assertThat(descFirst.entries()).extracting(StatementEntryRes::entryId)
                .doesNotContainAnyElementsOf(
                        descSecond.entries().stream().map(StatementEntryRes::entryId).toList());
    }
}
