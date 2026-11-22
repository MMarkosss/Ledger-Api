package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.accounts.dto.AccDetailRes;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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

    protected RestTestClient.ResponseSpec transferirSemHeader(UUID sourceAccountId,
                                                              UUID destinationAccountId,
                                                              String amount) {
        return rest.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sourceAccountId", sourceAccountId,
                        "destinationAccountId", destinationAccountId,
                        "amount", new BigDecimal(amount)))
                .exchange();
    }

    protected int transferirStatus(UUID sourceAccountId,
                                   UUID destinationAccountId,
                                   String key,
                                   String amount) {
        return transferir(sourceAccountId, destinationAccountId, key, amount)
                .returnResult()
                .getStatus()
                .value();
    }

    @SafeVarargs
    protected final List<Integer> executarJuntos(Callable<Integer>... tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (Callable<Integer> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(15, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
