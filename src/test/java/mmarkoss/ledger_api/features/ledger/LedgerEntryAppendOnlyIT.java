package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryAppendOnlyIT extends IntegrationTestSupport {

    @Test
    @DisplayName("UPDATE e DELETE de ledger_entry são rejeitados pela trigger append-only")
    void deveRejeitarMutacaoDeLancamento() {
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        jdbc.update("""
                insert into account (id, holder_name, document, balance)
                values (?, ?, ?, ?)
                """, accountId, "Maria Silva", UUID.randomUUID().toString(),
                new BigDecimal("100.00"));
        jdbc.update("""
                insert into transaction
                    (id, type, amount, idempotency_key, idempotency_payload_hash, status)
                values (?, 'DEPOSIT', ?, ?, ?, 'COMPLETED')
                """, transactionId, new BigDecimal("10.00"), "append-only-key", "hash");
        jdbc.update("""
                insert into ledger_entry
                    (transaction_id, account_id, direction, amount, balance_after)
                values (?, ?, 'C', ?, ?)
                """, transactionId, accountId, new BigDecimal("10.00"),
                new BigDecimal("100.00"));

        assertThatThrownBy(() -> jdbc.update(
                "update ledger_entry set amount = ? where transaction_id = ?",
                new BigDecimal("11.00"), transactionId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        assertThatThrownBy(() -> jdbc.update(
                "delete from ledger_entry where transaction_id = ?", transactionId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        assertThat(jdbc.queryForObject("select count(*) from ledger_entry", Long.class))
                .isEqualTo(1L);
    }
}
