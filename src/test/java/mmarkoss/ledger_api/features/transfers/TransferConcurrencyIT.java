package mmarkoss.ledger_api.features.transfers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "ledger.transfer.lock-timeout=1s")
class TransferConcurrencyIT extends TransferITSupport {

    private static final String HOLDER_THIRD = "Ana Lima";
    private static final String DOC_THIRD = "33333333333";

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

    private Connection segurarLock(UUID accountId) throws SQLException {
        Connection connection = jdbc.getDataSource().getConnection();
        connection.setAutoCommit(false);

        try (PreparedStatement statement = connection.prepareStatement(
                "select id from account where id = ? for update")) {
            statement.setObject(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Conta não encontrada para segurar o lock");
                }
            }
        }

        return connection;
    }

    private void liberarLock(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // O teste já falhou ou a conexão será descartada.
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Idem.
            }
        }
    }

    @Test
    @DisplayName("Chaves diferentes na mesma origem serializam sem deadlock")
    void deveProcessarChavesDiferentesNaMesmaOrigem() throws Exception {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID firstDestination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        UUID secondDestination = criarConta(HOLDER_THIRD, DOC_THIRD);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        List<Integer> statuses = executarJuntos(
                () -> transferirStatus(source, firstDestination, "trf-1", "30.00"),
                () -> transferirStatus(source, secondDestination, "trf-2", "20.00"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 201);
        assertThat(transferencias()).isEqualTo(2);
        assertThat(lancamentos()).isEqualTo(5);
        assertThat(saldo(source)).isEqualByComparingTo("50");
        assertThat(saldo(firstDestination)).isEqualByComparingTo("30");
        assertThat(saldo(secondDestination)).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Transferências em direções opostas não formam deadlock")
    void deveProcessarDirecoesOpostasSemDeadlock() throws Exception {
        UUID first = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID second = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(first, "dep-a", "100.00").expectStatus().isCreated();
        depositar(second, "dep-b", "100.00").expectStatus().isCreated();

        List<Integer> statuses = executarJuntos(
                () -> transferirStatus(first, second, "trf-ab", "30.00"),
                () -> transferirStatus(second, first, "trf-ba", "20.00"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 201);
        assertThat(transferencias()).isEqualTo(2);
        assertThat(lancamentos()).isEqualTo(6);
        assertThat(saldo(first)).isEqualByComparingTo("90");
        assertThat(saldo(second)).isEqualByComparingTo("110");
    }

    @Test
    @DisplayName("Lock timeout retorna 409 retryable com código distinto da idempotência")
    void deveMapearLockTimeoutPara409Retryable() throws Exception {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        Connection holder = segurarLock(source);
        try {
            transferir(source, destination, "trf-lock", "10.00")
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Conflito de concorrência")
                    .jsonPath("$.code").isEqualTo("CONCURRENCY_CONFLICT")
                    .jsonPath("$.retryable").isEqualTo(true);
        } finally {
            liberarLock(holder);
        }

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isEqualTo(1);
        assertThat(saldo(source)).isEqualByComparingTo("100");
        assertThat(saldo(destination)).isEqualByComparingTo("0");
    }
}
