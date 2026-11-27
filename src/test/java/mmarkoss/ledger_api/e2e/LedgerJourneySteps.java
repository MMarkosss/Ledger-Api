package mmarkoss.ledger_api.e2e;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import mmarkoss.ledger_api.features.accounts.dto.AccDetailRes;
import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.ledger.EntryDirection;
import mmarkoss.ledger_api.features.ledger.TransactionType;
import mmarkoss.ledger_api.features.statements.dto.StatementEntryRes;
import mmarkoss.ledger_api.features.statements.dto.StatementPageRes;
import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LedgerJourneySteps {

    private static final String HOLDER_SOURCE = "Maria Silva";
    private static final String HOLDER_DESTINATION = "João Souza";
    private static final String DOC_SOURCE = "11111111111";
    private static final String DOC_DESTINATION = "22222222222";

    @Autowired
    private RestTestClient rest;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID source;
    private UUID destination;
    private UUID transferTransactionId;
    private String depositAmount;
    private String transferAmount;
    private StatementPageRes sourceStatement;
    private StatementPageRes destinationStatement;

    @Before
    public void limparBanco() {
        jdbc.update("truncate table ledger_entry, transaction, account restart identity");
    }

    @Dado("que criei uma conta de origem com saldo {string}")
    public void criarContaDeOrigemComSaldo(String amount) {
        source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        depositAmount = amount;
        depositar(source, "e2e-deposit", amount);
    }

    @E("criei uma conta de destino")
    public void criarContaDeDestino() {
        destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);
    }

    @Quando("transfiro {string} da origem para o destino com a chave {string}")
    public void transferir(String amount, String key) {
        TransferRes result = transferir(source, destination, key, amount, 201);
        transferTransactionId = result.transactionId();
        transferAmount = amount;
    }

    @E("repito a mesma transferência com a chave {string}")
    public void repetirTransferencia(String key) {
        TransferRes result = transferir(source, destination, key, transferAmount, 200);
        assertThat(result.transactionId()).isEqualTo(transferTransactionId);
    }

    @Então("a conta de origem tem saldo {string}")
    public void contaDeOrigemTemSaldo(String expected) {
        assertThat(saldo(source)).isEqualByComparingTo(expected);
    }

    @E("a conta de destino tem saldo {string}")
    public void contaDeDestinoTemSaldo(String expected) {
        assertThat(saldo(destination)).isEqualByComparingTo(expected);
    }

    @E("o extrato da origem mostra o depósito e a transferência em ordem decrescente")
    public void extratoDaOrigem() {
        sourceStatement = extrato(source);

        assertThat(sourceStatement.order()).isEqualTo("desc");
        assertThat(sourceStatement.entries()).hasSize(2);
        assertThat(sourceStatement.hasMore()).isFalse();
        assertThat(sourceStatement.nextCursor()).isNull();

        StatementEntryRes transfer = sourceStatement.entries().getFirst();
        assertThat(transfer.transactionId()).isEqualTo(transferTransactionId);
        assertThat(transfer.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(transfer.direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(transfer.amount()).isEqualByComparingTo(transferAmount);
        assertThat(transfer.balanceAfter()).isEqualByComparingTo(
                new BigDecimal(depositAmount).subtract(new BigDecimal(transferAmount)));
        assertThat(transfer.counterpartyId()).isEqualTo(destination);

        StatementEntryRes deposit = sourceStatement.entries().get(1);
        assertThat(deposit.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(deposit.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(deposit.amount()).isEqualByComparingTo(depositAmount);
        assertThat(deposit.balanceAfter()).isEqualByComparingTo(depositAmount);
        assertThat(deposit.counterpartyId()).isNull();
    }

    @E("o extrato do destino mostra a transferência recebida")
    public void extratoDoDestino() {
        destinationStatement = extrato(destination);

        assertThat(destinationStatement.entries()).hasSize(1);
        StatementEntryRes credit = destinationStatement.entries().getFirst();
        assertThat(credit.transactionId()).isEqualTo(transferTransactionId);
        assertThat(credit.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(credit.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo(transferAmount);
        assertThat(credit.balanceAfter()).isEqualByComparingTo(transferAmount);
        assertThat(credit.counterpartyId()).isEqualTo(source);
    }

    @E("os extratos refletem o mesmo par de transferência")
    public void extratosRefletemOMesmoPar() {
        StatementEntryRes debit = transferEntry(sourceStatement);
        StatementEntryRes credit = transferEntry(destinationStatement);

        assertThat(debit.transactionId()).isEqualTo(transferTransactionId);
        assertThat(credit.transactionId()).isEqualTo(transferTransactionId);
        assertThat(debit.transactionId()).isEqualTo(credit.transactionId());
        assertThat(debit.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(credit.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(debit.amount()).isEqualByComparingTo(credit.amount());
        assertThat(debit.amount()).isEqualByComparingTo(transferAmount);
        assertThat(debit.direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(credit.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(debit.counterpartyId()).isEqualTo(destination);
        assertThat(credit.counterpartyId()).isEqualTo(source);
    }

    @E("a cadeia de balance_after de cada extrato é contínua")
    public void cadeiaDeBalanceAfterContinua() {
        assertBalanceChain(sourceStatement);
        assertBalanceChain(destinationStatement);
    }

    @E("o saldo final de cada conta é igual ao balance_after mais recente")
    public void saldoFinalIgualAoBalanceAfterMaisRecente() {
        assertFinalBalance(source, sourceStatement);
        assertFinalBalance(destination, destinationStatement);
    }

    @E("os lançamentos do extrato têm data de criação")
    public void lancamentosTemDataDeCriacao() {
        assertThat(sourceStatement.entries())
                .allSatisfy(entry -> assertThat(entry.createdAt()).isNotNull());
        assertThat(destinationStatement.entries())
                .allSatisfy(entry -> assertThat(entry.createdAt()).isNotNull());
    }

    private StatementEntryRes transferEntry(StatementPageRes statement) {
        return statement.entries().stream()
                .filter(entry -> entry.transactionId().equals(transferTransactionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Transferência " + transferTransactionId + " não encontrada no extrato"));
    }

    private void assertBalanceChain(StatementPageRes statement) {
        List<StatementEntryRes> entries = new ArrayList<>(statement.entries());
        entries.sort(Comparator.comparing(StatementEntryRes::entryId));

        BigDecimal running = BigDecimal.ZERO;
        for (StatementEntryRes entry : entries) {
            running = entry.direction() == EntryDirection.CREDIT
                    ? running.add(entry.amount())
                    : running.subtract(entry.amount());

            assertThat(entry.balanceAfter())
                    .as("balanceAfter do lançamento %s", entry.entryId())
                    .isEqualByComparingTo(running);
        }
    }

    private void assertFinalBalance(UUID accountId, StatementPageRes statement) {
        StatementEntryRes newest = statement.entries().stream()
                .max(Comparator.comparing(StatementEntryRes::entryId))
                .orElseThrow(() -> new AssertionError(
                        "Extrato vazio para a conta " + accountId));

        assertThat(saldo(accountId))
                .as("saldo final da conta %s", accountId)
                .isEqualByComparingTo(newest.balanceAfter());
    }

    private UUID criarConta(String holder, String document) {
        return rest.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccPostReq(holder, document))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccDetailRes.class)
                .returnResult().getResponseBody().id();
    }

    private void depositar(UUID accountId, String key, String amount) {
        rest.post().uri("/accounts/{id}/deposits", accountId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", new BigDecimal(amount)))
                .exchange()
                .expectStatus().isCreated();
    }

    private TransferRes transferir(UUID sourceAccountId,
                                   UUID destinationAccountId,
                                   String key,
                                   String amount,
                                   int expectedStatus) {
        return rest.post().uri("/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sourceAccountId", sourceAccountId,
                        "destinationAccountId", destinationAccountId,
                        "amount", new BigDecimal(amount)))
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody(TransferRes.class)
                .returnResult().getResponseBody();
    }

    private BigDecimal saldo(UUID accountId) {
        return rest.get().uri("/accounts/{id}", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccDetailRes.class)
                .returnResult().getResponseBody().balance();
    }

    private StatementPageRes extrato(UUID accountId) {
        return rest.get().uri("/accounts/{id}/statements", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StatementPageRes.class)
                .returnResult().getResponseBody();
    }
}
