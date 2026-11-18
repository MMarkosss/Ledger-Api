package mmarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class AccountReadIT extends AccountITSupport {

    @Test
    @DisplayName("GET de id inexistente retorna 404 com title (P1 - not found)")
    void deveRetornar404ParaIdInexistente() {
        rest.get().uri("/accounts/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada");
    }
}
