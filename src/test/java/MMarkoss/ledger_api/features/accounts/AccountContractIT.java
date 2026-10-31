package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.TestcontainersConfiguration;
import MMarkoss.ledger_api.features.accounts.dto.AccPostReq;
import MMarkoss.ledger_api.features.accounts.dto.AccPostRes;
import MMarkoss.ledger_api.features.accounts.dto.AccUpdateRes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de CONTRATO HTTP: prova o comportamento externo da API (status + corpo),
 * atravessando Controller -> Service -> Repository -> Postgres real (Testcontainers).
 * Prova em runtime os itens P1 (erros), P2 (saldo), P3 (document) e P4 (imutabilidade).
 * Provas unitárias do P5 ficam no AccountBalanceOperatorTest (T-B).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class AccountContractIT {

    private static final String HOLDER = "Maria Silva";
    private static final String DOC = "12345678901";

    @Autowired
    RestTestClient rest;
    @Autowired
    AccountRepository accRepo;

    @BeforeEach
    void limparBanco() {
        // cliente HTTP real => cada request comita; rollback não existe aqui.
        // deleteAll garante estado vazio por teste, evitando colisão de document UNIQUE.
        accRepo.deleteAll();
    }

    private RestTestClient.ResponseSpec post(Object body) {
        return rest.post().uri("/contas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private UUID criarContaValida() {
        return post(new AccPostReq(HOLDER, DOC))
                .expectStatus().isCreated()
                .expectBody(AccPostRes.class)
                .returnResult().getResponseBody().id();
    }

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

    @Test
    @DisplayName("GET de id inexistente retorna 404 com title (P1 - not found)")
    void deveRetornar404ParaIdInexistente() {
        rest.get().uri("/contas/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Conta não encontrada");
    }

    @Test
    @DisplayName("PUT não altera document, mesmo quando o cliente envia (P4)")
    void putNaoAlteraDocument() {
        UUID id = criarContaValida();

        // Contrato não permite mutar document: o PUT pode 200 (Jackson ignora campo
        // desconhecido) ou erro (D2 pendente) — o invariante testado é o document.
        rest.put().uri("/contas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("holderName", "Novo Nome", "document", "99999999999"))
                .exchange();

        rest.get().uri("/contas/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.document").isEqualTo(DOC);
    }

    @Test
    @DisplayName("PUT devolve id da conta atualizada (D3)")
    void putDevolveId() {
        UUID id = criarContaValida();

        AccUpdateRes body = rest.put().uri("/contas/{id}", id)
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

        AccUpdateRes body = rest.patch().uri("/contas/{id}", id)
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
