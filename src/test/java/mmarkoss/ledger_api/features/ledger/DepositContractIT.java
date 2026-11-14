package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.TestcontainersConfiguration;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.accounts.dto.AccPostRes;
import mmarkoss.ledger_api.features.ledger.dto.DepositRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class DepositContractIT {

    private static final String HOLDER = "Maria Silva";
    private static final String DOC = "12345678901";

    @Autowired
    RestTestClient rest;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.update("delete from ledger_entry");
        jdbc.update("delete from transaction");
        jdbc.update("delete from account");
    }

    private UUID criarConta() {
        return rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccPostReq(HOLDER, DOC))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccPostRes.class)
                .returnResult().getResponseBody().id();
    }

    private RestTestClient.ResponseSpec depositar(UUID accountId, String key, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange();
    }

    private RestTestClient.ResponseSpec depositarSemHeader(UUID accountId, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange();
    }

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
