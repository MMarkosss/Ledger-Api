package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AccountCreationIT extends AccountITSupport {

    @Test
    @DisplayName("POST cria conta com saldo ZERO: cliente não controla balance (P2)")
    void deveCriarContaComSaldoZero() {
        post(new AccPostReq(HOLDER, DOC))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.balance").isEqualTo(0);
    }

    @Test
    @DisplayName("POST com document duplicado retorna 409 (P1 - conflito)")
    void deveRejeitarDocumentDuplicado() {
        criarContaValida();

        post(new AccPostReq(HOLDER, DOC))
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conflito");
    }

    @Test
    @DisplayName("POST inválido retorna 400 com mapa de erros; document null rejeitado (P1 - validação / P3)")
    void deveRejeitarCamposInvalidos() {
        post(new AccPostReq("Ab", DOC))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.holderName").exists();

        post(new AccPostReq(HOLDER, null))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.document").exists();
    }
}
