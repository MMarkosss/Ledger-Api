package mmarkoss.ledger_api.features.deposits;

import mmarkoss.ledger_api.features.deposits.dto.DepositRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DepositIdempotencyIT extends DepositITSupport {

    @Test
    @DisplayName("Replay com mesma chave e mesmo hash retorna 200 e balance_after armazenado")
    void deveReplayComSaldoArmazenado() {
        UUID accountId = criarConta();

        DepositRes first = depositar(accountId, "dep-1", "100.00")
                .expectStatus().isCreated()
                .expectBody(DepositRes.class)
                .returnResult().getResponseBody();

        depositar(accountId, "dep-2", "50.00")
                .expectStatus().isCreated();

        DepositRes replay = depositar(accountId, "dep-1", "100.0")
                .expectStatus().isOk()
                .expectBody(DepositRes.class)
                .returnResult().getResponseBody();

        assertThat(replay.transactionId()).isEqualTo(first.transactionId());
        assertThat(replay.balance()).isEqualByComparingTo("100");

        rest.get().uri("/accounts/{id}", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(150.0);
    }

    @Test
    @DisplayName("Mesma Idempotency-Key com hash diferente retorna 409")
    void deveRejeitarHashDiferente() {
        UUID accountId = criarConta();

        depositar(accountId, "dep-1", "100.00")
                .expectStatus().isCreated();

        depositar(accountId, "dep-1", "100.01")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflito de idempotência");
    }
}
