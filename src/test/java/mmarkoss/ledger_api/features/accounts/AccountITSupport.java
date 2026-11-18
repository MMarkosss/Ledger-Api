package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.accounts.dto.AccPostRes;
import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

/**
 * Helpers comuns dos ITs de conta. O comportamento de cada classe fica no teste;
 * aqui vive apenas a criação da conta e o POST repetido.
 */
abstract class AccountITSupport extends IntegrationTestSupport {

    protected static final String HOLDER = "Maria Silva";
    protected static final String DOC = "12345678901";

    protected RestTestClient.ResponseSpec post(Object body) {
        return rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    protected UUID criarContaValida() {
        return post(new AccPostReq(HOLDER, DOC))
                .expectStatus().isCreated()
                .expectBody(AccPostRes.class)
                .returnResult().getResponseBody().id();
    }
}
