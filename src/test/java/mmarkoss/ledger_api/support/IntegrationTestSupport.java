package mmarkoss.ledger_api.support;

import mmarkoss.ledger_api.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base dos testes de integração: sobe o contexto completo uma vez e limpa o banco
 * antes de cada teste.
 *
 * O ledger é append-only no banco (ADR-009), então DELETE em {@code ledger_entry} é
 * rejeitado pela trigger. TRUNCATE não dispara row triggers; a lista explícita das três
 * tabelas evita CASCADE e mantém a dependência de FK visível.
 *
 * Cliente HTTP real comita cada request; por isso a limpeza é explícita e não depende
 * de rollback transacional.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected RestTestClient rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    protected final void cleanDatabase() {
        jdbc.update("truncate table ledger_entry, transaction, account restart identity");
    }
}
