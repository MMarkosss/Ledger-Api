package mmarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

class AccountDeletionIT extends AccountITSupport {

    private static final String IDEMPOTENCY_KEY = "delete-account-deposit";

    @Test
    @DisplayName("DELETE remove conta sem lançamentos")
    void deveRemoverContaSemLancamentos() {
        UUID id = criarContaValida();

        rest.delete().uri("/accounts/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        rest.get().uri("/accounts/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("DELETE de conta com lançamento retorna 409 e preserva a conta")
    void deveBloquearContaComLancamento() {
        UUID id = criarContaValida();
        depositar(id);

        rest.delete().uri("/accounts/{id}", id)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta com lançamentos");

        rest.get().uri("/accounts/{id}", id)
                .exchange()
                .expectStatus().isOk();
    }

    private void depositar(UUID accountId) {
        rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isCreated();
    }
}
