package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferHappyPathIT extends TransferITSupport {

    @Test
    @DisplayName("POST transfere e grava débito + crédito com snapshot dos saldos")
    void deveTransferirEGravarDuasPernas() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-source", "100.00").expectStatus().isCreated();

        TransferRes transfer = transferir(source, destination, "trf-1", "30.00")
                .expectStatus().isCreated()
                .expectBody(TransferRes.class)
                .returnResult().getResponseBody();

        assertThat(transfer.transactionId()).isNotNull();
        assertThat(transfer.sourceBalance()).isEqualByComparingTo("70");
        assertThat(transfer.destinationBalance()).isEqualByComparingTo("30");

        rest.get().uri("/accounts/{id}", source)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(70.0);

        rest.get().uri("/accounts/{id}", destination)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(30.0);

        assertThat(jdbc.queryForObject(
                "select type from transaction where id = ?",
                String.class,
                transfer.transactionId()))
                .isEqualTo("TRANSFER");
        assertThat(jdbc.queryForObject(
                "select amount from transaction where id = ?",
                BigDecimal.class,
                transfer.transactionId()))
                .isEqualByComparingTo("30");
        assertThat(jdbc.queryForObject(
                "select count(*) from ledger_entry",
                Long.class))
                .isEqualTo(3L);

        Map<String, Object> debit = jdbc.queryForMap("""
                select direction, amount, balance_after, account_id, counterparty_id
                from ledger_entry
                where transaction_id = ? and direction = 'D'
                """, transfer.transactionId());
        assertThat(debit.get("direction")).isEqualTo("D");
        assertThat((BigDecimal) debit.get("amount")).isEqualByComparingTo("30");
        assertThat((BigDecimal) debit.get("balance_after")).isEqualByComparingTo("70");
        assertThat(debit.get("account_id")).isEqualTo(source);
        assertThat(debit.get("counterparty_id")).isEqualTo(destination);

        Map<String, Object> credit = jdbc.queryForMap("""
                select direction, amount, balance_after, account_id, counterparty_id
                from ledger_entry
                where transaction_id = ? and direction = 'C'
                """, transfer.transactionId());
        assertThat(credit.get("direction")).isEqualTo("C");
        assertThat((BigDecimal) credit.get("amount")).isEqualByComparingTo("30");
        assertThat((BigDecimal) credit.get("balance_after")).isEqualByComparingTo("30");
        assertThat(credit.get("account_id")).isEqualTo(destination);
        assertThat(credit.get("counterparty_id")).isEqualTo(source);
    }
}
