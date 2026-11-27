package mmarkoss.ledger_api.features.statements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatementValidationIT extends StatementITSupport {

    @Test
    @DisplayName("Conta inexistente retorna 404")
    void deveRetornarNotFoundParaContaInexistente() {
        extrato(UUID.randomUUID(), "")
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Conta existente sem lançamentos retorna página vazia")
    void deveRetornarPaginaVaziaParaContaSemLancamentos() {
        UUID account = criarConta(HOLDER_SOURCE, DOC_SOURCE);

        var page = extratoOk(account, "");

        assertThat(page.order()).isEqualTo("desc");
        assertThat(page.entries()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("Order inválido retorna 400")
    void deveRejeitarOrderInvalido() {
        UUID account = criarConta(HOLDER_SOURCE, DOC_SOURCE);

        extrato(account, "?order=sideways")
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Limite fora da faixa ou não numérico retorna 400")
    void deveRejeitarLimiteInvalido() {
        UUID account = criarConta(HOLDER_SOURCE, DOC_SOURCE);

        extrato(account, "?limit=0").expectStatus().isBadRequest();
        extrato(account, "?limit=101").expectStatus().isBadRequest();
        extrato(account, "?limit=abc").expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Cursor inválido ou não numérico retorna 400")
    void deveRejeitarCursorInvalido() {
        UUID account = criarConta(HOLDER_SOURCE, DOC_SOURCE);

        extrato(account, "?cursor=0").expectStatus().isBadRequest();
        extrato(account, "?cursor=-1").expectStatus().isBadRequest();
        extrato(account, "?cursor=abc").expectStatus().isBadRequest();
    }
}
