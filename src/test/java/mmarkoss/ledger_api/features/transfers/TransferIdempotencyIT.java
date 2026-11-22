package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferIdempotencyIT extends TransferITSupport {

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
    @DisplayName("Mesma chave e payload retorna 200 com os balance_after armazenados")
    void deveReplayComSaldosArmazenados() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        TransferRes first = transferir(source, destination, "trf-1", "30.00")
                .expectStatus().isCreated()
                .expectBody(TransferRes.class)
                .returnResult().getResponseBody();

        depositar(source, "dep-2", "50.00").expectStatus().isCreated();

        TransferRes replay = transferir(source, destination, "trf-1", "30.0000")
                .expectStatus().isOk()
                .expectBody(TransferRes.class)
                .returnResult().getResponseBody();

        assertThat(replay.transactionId()).isEqualTo(first.transactionId());
        assertThat(replay.sourceBalance()).isEqualByComparingTo("70");
        assertThat(replay.destinationBalance()).isEqualByComparingTo("30");

        assertThat(saldo(source)).isEqualByComparingTo("120");
        assertThat(saldo(destination)).isEqualByComparingTo("30");
        assertThat(transferencias()).isEqualTo(1);
        assertThat(lancamentos()).isEqualTo(4);

        assertThat(jdbc.queryForObject(
                "select count(*) from ledger_entry where transaction_id = ?",
                Long.class,
                first.transactionId()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Mesma chave com payload diferente retorna 409 sem nova transferência")
    void deveRejeitarPayloadDiferente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        transferir(source, destination, "trf-1", "30.00")
                .expectStatus().isCreated();

        transferir(source, destination, "trf-1", "40.00")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflito de idempotência")
                .jsonPath("$.code").isEqualTo("IDEMPOTENCY_CONFLICT");

        transferir(destination, source, "trf-1", "30.00")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflito de idempotência")
                .jsonPath("$.code").isEqualTo("IDEMPOTENCY_CONFLICT");

        assertThat(transferencias()).isEqualTo(1);
        assertThat(lancamentos()).isEqualTo(3);
        assertThat(saldo(source)).isEqualByComparingTo("70");
        assertThat(saldo(destination)).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Chave usada por depósito retorna 409 de tipo diferente")
    void deveRejeitarTipoDiferente() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "shared-key", "100.00").expectStatus().isCreated();

        transferir(source, destination, "shared-key", "30.00")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflito de idempotência")
                .jsonPath("$.code").isEqualTo("IDEMPOTENCY_CONFLICT");

        assertThat(transferencias()).isZero();
        assertThat(lancamentos()).isEqualTo(1);
        assertThat(saldo(source)).isEqualByComparingTo("100");
        assertThat(saldo(destination)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Mesma chave concorrente cria uma única transferência e um replay")
    void deveProcessarMesmaChaveConcorrenteUmaVez() throws Exception {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
        depositar(source, "dep-1", "100.00").expectStatus().isCreated();

        List<Integer> statuses = executarJuntos(
                () -> transferirStatus(source, destination, "trf-1", "30.00"),
                () -> transferirStatus(source, destination, "trf-1", "30.00"));

        assertThat(statuses).containsExactlyInAnyOrder(201, 200);
        assertThat(transferencias()).isEqualTo(1);
        assertThat(lancamentos()).isEqualTo(3);
        assertThat(saldo(source)).isEqualByComparingTo("70");
        assertThat(saldo(destination)).isEqualByComparingTo("30");
    }
}
