package mmarkoss.ledger_api.features.transfers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rejeições da transferência: mesma conta, contas inexistentes, saldo insuficiente,
 * header e amount inválidos. O caminho feliz e a idempotência têm classes próprias.
 */
class TransferValidationIT extends TransferITSupport {

    private long transferencias() {
        return jdbc.queryForObject(
                "select count(*) from transaction where type = 'TRANSFER'",
                Long.class);
    }

    private long lancamentos() {
        return jdbc.queryForObject("select count(*) from ledger_entry", Long.class);
    }

    private BigDecimal saldo(UUID accountId) {
        return jdbc.queryForObject(
                "select balance from account where id = ?", BigDecimal.class, accountId);
    }

    @Test
    @DisplayName("Transferência para a mesma conta retorna 422 sem transação")
    void deveRejeitarMesmaConta() {
        UUID accountId = criarConta(HOLDER_SOURCE, DOC_SOURCE);

        transferir(accountId, accountId, "trf-1", "10.00")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Transferência inválida");

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isZero();
    }

    @Test
    @DisplayName("Origem inexistente retorna 404 sem transação")
    void deveRejeitarOrigemInexistente() {
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        transferir(UUID.randomUUID(), destination, "trf-1", "10.00")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada");

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isZero();
    }

    @Test
    @DisplayName("Destino inexistente retorna 404 e preserva o saldo da origem")
    void deveRejeitarDestinoInexistente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        transferir(source, UUID.randomUUID(), "trf-1", "10.00")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada");

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isEqualTo(1);
        assertThat(saldo(source)).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Saldo insuficiente retorna 422 e faz rollback da transferência")
    void deveRejeitarSaldoInsuficiente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-1", "10.00").expectStatus().isCreated();

        transferir(source, destination, "trf-1", "30.00")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Saldo insuficiente");

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isEqualTo(1);
        assertThat(saldo(source)).isEqualByComparingTo("10");
        assertThat(saldo(destination)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Idempotency-Key ausente retorna 400")
    void deveRejeitarHeaderAusente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        transferirSemHeader(source, destination, "10.00")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Requisição inválida");

        assertThat(transferencias()).isZero();
    }

    @Test
    @DisplayName("Idempotency-Key em branco retorna 400")
    void deveRejeitarHeaderEmBranco() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        transferir(source, destination, "   ", "10.00")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Requisição inválida");

        assertThat(transferencias()).isZero();
    }

    @Test
    @DisplayName("Valor inválido no domínio retorna 422")
    void deveRejeitarValorInvalido() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        transferir(source, destination, "trf-1", "-10.00")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Valor inválido");

        transferir(source, destination, "trf-2", "0")
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Valor inválido");

        assertThat(transferencias()).isZero();
    }

    @Test
    @DisplayName("Valor ausente retorna 400 com mapa de erros")
    void deveRejeitarValorAusente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        rest.post().uri("/transfers")
                .header("Idempotency-Key", "trf-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sourceAccountId", source,
                        "destinationAccountId", destination))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.amount").exists();

        assertThat(transferencias()).isZero();
    }

    @Test
    @DisplayName("Conta de origem ausente no body retorna 400 com mapa de erros")
    void deveRejeitarOrigemAusente() {
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        rest.post().uri("/transfers")
                .header("Idempotency-Key", "trf-1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "destinationAccountId", destination,
                        "amount", new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.sourceAccountId").exists();

        assertThat(transferencias()).isZero();
    }
}
