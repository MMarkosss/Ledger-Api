package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.TestcontainersConfiguration;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.accounts.dto.AccPostRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class DepositConcurrencyIT {

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

    private int depositarStatus(UUID accountId, String key, String amount) {
        return rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange()
                .returnResult()
                .getStatus()
                .value();
    }

    @SafeVarargs
    private List<Integer> executarJuntos(Callable<Integer>... tasks) throws Exception {
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

    private long contar(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private BigDecimal saldo(UUID accountId) {
        return jdbc.queryForObject(
                "select balance from account where id = ?", BigDecimal.class, accountId);
    }

    @Test
    @DisplayName("Mesma Idempotency-Key concorrente cria um único lançamento")
    void deveProcessarMesmaChaveUmaVez() throws Exception {
        UUID accountId = criarConta();

        List<Integer> statuses = executarJuntos(
                () -> depositarStatus(accountId, "dep-1", "100.00"),
                () -> depositarStatus(accountId, "dep-1", "100.00"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 200);
        assertThat(contar("transaction")).isEqualTo(1);
        assertThat(contar("ledger_entry")).isEqualTo(1);
        assertThat(saldo(accountId)).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Chaves diferentes concorrentes somam os dois depósitos")
    void deveSomarDepositosConcorrentes() throws Exception {
        UUID accountId = criarConta();

        List<Integer> statuses = executarJuntos(
                () -> depositarStatus(accountId, "dep-1", "100.00"),
                () -> depositarStatus(accountId, "dep-2", "50.00"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 201);
        assertThat(contar("transaction")).isEqualTo(2);
        assertThat(contar("ledger_entry")).isEqualTo(2);
        assertThat(saldo(accountId)).isEqualByComparingTo("150");
    }
}
