package mmarkoss.ledger_api.features.accounts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AccountErrorIT extends AccountITSupport {

    @Test
    @DisplayName("Corpo não desserializável retorna 400 (D2)")
    void deveRejeitarCorpoIlegivel() {
        post(Map.of("holderName", Map.of("invalido", true), "document", DOC))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Requisição inválida");
    }
}
