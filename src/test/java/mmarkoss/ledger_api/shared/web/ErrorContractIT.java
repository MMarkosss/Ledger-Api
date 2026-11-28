package mmarkoss.ledger_api.shared.web;

import mmarkoss.ledger_api.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

class ErrorContractIT extends IntegrationTestSupport {

    @Test
    @DisplayName("Rota inexistente retorna 404 com traceId propagado e sem stacktrace")
    void deveRetornar404ParaRotaInexistente() {
        rest.get().uri("/does-not-exist")
                .header(TraceIdFilter.HEADER, "trace-fixo")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().valueEquals(TraceIdFilter.HEADER, "trace-fixo")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Recurso não encontrado")
                .jsonPath("$.detail").isEqualTo("Recurso não encontrado")
                .jsonPath("$.traceId").isEqualTo("trace-fixo")
                .jsonPath("$.message").doesNotExist()
                .jsonPath("$.stackTrace").doesNotExist();
    }

    @Test
    @DisplayName("Erro de domínio inclui traceId gerado")
    void deveIncluirTraceIdEmErroDeDominio() {
        rest.get().uri("/accounts/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().exists(TraceIdFilter.HEADER)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada")
                .jsonPath("$.traceId").isNotEmpty();
    }

    @Test
    @DisplayName("Erro de validação inclui traceId e erros de campo")
    void deveIncluirTraceIdEmErroDeValidacao() {
        rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().exists(TraceIdFilter.HEADER)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Dados inválidos")
                .jsonPath("$.traceId").isNotEmpty()
                .jsonPath("$.errors.holderName").exists()
                .jsonPath("$.errors.document").exists();
    }
}
