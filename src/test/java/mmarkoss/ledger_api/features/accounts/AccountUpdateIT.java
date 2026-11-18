package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.features.accounts.dto.AccUpdateRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountUpdateIT extends AccountITSupport {

    @Test
    @DisplayName("PUT não altera document, mesmo quando o cliente envia (P4)")
    void putNaoAlteraDocument() {
        UUID id = criarContaValida();

        // Contrato não permite mutar document: o PUT pode 200 (Jackson ignora campo
        // desconhecido) ou erro (D2 pendente) — o invariante testado é o document.
        rest.put().uri("/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("holderName", "Novo Nome", "document", "99999999999"))
                .exchange();

        rest.get().uri("/accounts/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.document").isEqualTo(DOC);
    }

    @Test
    @DisplayName("PUT devolve id da conta atualizada (D3)")
    void putDevolveId() {
        UUID id = criarContaValida();

        AccUpdateRes body = rest.put().uri("/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("holderName", "Novo Nome"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccUpdateRes.class)
                .returnResult().getResponseBody();

        assertThat(body.id()).isEqualTo(id);
        assertThat(body.holderName()).isEqualTo("Novo Nome");
    }

    @Test
    @DisplayName("PATCH devolve id da conta atualizada (D3)")
    void patchDevolveId() {
        UUID id = criarContaValida();

        AccUpdateRes body = rest.patch().uri("/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("holderName", "Novo Nome"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccUpdateRes.class)
                .returnResult().getResponseBody();

        assertThat(body.id()).isEqualTo(id);
        assertThat(body.holderName()).isEqualTo("Novo Nome");
    }
}
