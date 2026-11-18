package mmarkoss.ledger_api.features.deposits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DepositHappyPathIT extends DepositITSupport {

    @Test
    @DisplayName("POST deposita e grava transaction + ledger_entry atomicamente")
    void deveDepositarEGravarLancamento() {
        UUID accountId = criarConta();

        depositar(accountId, "dep-1", "100.50")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.transactionId").isNotEmpty()
                .jsonPath("$.balance").isEqualTo(100.5);

        rest.get().uri("/accounts/{id}", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(100.5);

        assertThat(jdbc.queryForObject("select count(*) from transaction", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from ledger_entry", Long.class))
                .isEqualTo(1L);
    }
}
