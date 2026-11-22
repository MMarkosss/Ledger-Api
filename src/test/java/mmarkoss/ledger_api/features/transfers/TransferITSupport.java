package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.accounts.dto.AccDetailRes;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Helpers comuns dos ITs de transferência. O comportamento de cada classe fica no teste;
 * aqui vive apenas a criação das contas e as chamadas HTTP repetidas.
 */
abstract class TransferITSupport extends IntegrationTestSupport {

    protected static final String HOLDER_SOURCE = "Maria Silva";
    protected static final String HOLDER_DESTINATION = "João Souza";
    protected static final String DOC_SOURCE = "11111111111";
    protected static final String DOC_DESTINATION = "22222222222";

    protected UUID criarConta(String holder, String document) {
        return rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccPostReq(holder, document))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccDetailRes.class)
                .returnResult().getResponseBody().id();
    }

    protected RestTestClient.ResponseSpec depositar(UUID accountId, String key, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange();
    }

    protected RestTestClient.ResponseSpec transferir(UUID sourceAccountId,
                                                     UUID destinationAccountId,
                                                     String key,
                                                     String amount) {
        return rest.post().uri("/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sourceAccountId", sourceAccountId,
                        "destinationAccountId", destinationAccountId,
                        "amount", new BigDecimal(amount)))
                .exchange();
    }
}
