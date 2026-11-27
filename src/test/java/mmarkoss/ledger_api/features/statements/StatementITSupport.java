package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.features.accounts.dto.AccDetailRes;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.statements.dto.StatementPageRes;
import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Helpers comuns dos ITs de extrato. Cada classe de teste mantém as asserções do seu
 * comportamento; aqui ficam apenas as chamadas HTTP repetidas.
 */
abstract class StatementITSupport extends IntegrationTestSupport {

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

    protected void depositar(UUID accountId, String key, String amount) {
        rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange()
                .expectStatus().isCreated();
    }

    protected TransferRes transferir(UUID sourceAccountId,
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
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TransferRes.class)
                .returnResult().getResponseBody();
    }

    protected RestTestClient.ResponseSpec extrato(UUID accountId, String query) {
        return rest.get()
                .uri("/accounts/{id}/statements" + query, accountId)
                .exchange();
    }

    protected StatementPageRes extratoOk(UUID accountId, String query) {
        return extrato(accountId, query)
                .expectStatus().isOk()
                .expectBody(StatementPageRes.class)
                .returnResult().getResponseBody();
    }
}
