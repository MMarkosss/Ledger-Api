package mmarkoss.ledger_api.features.deposits;

import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.accounts.dto.AccPostRes;
import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Helpers comuns dos ITs de depósito. O comportamento de cada classe fica no teste;
 * aqui vive apenas a criação da conta e as chamadas HTTP repetidas.
 */
abstract class DepositITSupport extends IntegrationTestSupport {

    protected static final String HOLDER = "Maria Silva";
    protected static final String DOC = "12345678901";

    protected UUID criarConta() {
        return rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccPostReq(HOLDER, DOC))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccPostRes.class)
                .returnResult().getResponseBody().id();
    }

    protected RestTestClient.ResponseSpec depositar(UUID accountId, String key, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange();
    }

    protected RestTestClient.ResponseSpec depositarSemHeader(UUID accountId, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange();
    }
}
