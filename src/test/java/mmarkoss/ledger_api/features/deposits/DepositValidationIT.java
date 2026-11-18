package mmarkoss.ledger_api.features.deposits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Rejeições do depósito: validação de entrada, conta inexistente e valor inválido.
 * O caminho feliz e a idempotência têm classes próprias.
 */
class DepositValidationIT extends DepositITSupport {

    @Test
    @DisplayName("Depósito em conta inexistente retorna 404")
    void deveRejeitarContaInexistente() {
        depositar(UUID.randomUUID(), "dep-1", "10.00")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada");
    }

    @Test
    @DisplayName("Idempotency-Key ausente retorna 400")
    void deveRejeitarHeaderAusente() {
        UUID accountId = criarConta();

        depositarSemHeader(accountId, "10.00")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Requisição inválida");
    }

    @Test
    @DisplayName("Idempotency-Key em branco retorna 400")
    void deveRejeitarHeaderEmBranco() {
        UUID accountId = criarConta();

        rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", "   ")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Requisição inválida");
    }

    @Test
    @DisplayName("Valor inválido no domínio retorna 422")
    void deveRejeitarValorInvalido() {
        UUID accountId = criarConta();

        depositar(accountId, "dep-1", "-10.00")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Valor inválido");

        depositar(accountId, "dep-2", "0")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Valor inválido");
    }

    @Test
    @DisplayName("Valor ausente retorna 400 com mapa de erros")
    void deveRejeitarValorAusente() {
        UUID accountId = criarConta();

        rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", "dep-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.amount").exists();
    }
}
