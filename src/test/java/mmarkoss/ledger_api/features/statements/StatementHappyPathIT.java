package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.features.ledger.EntryDirection;
import mmarkoss.ledger_api.features.ledger.TransactionType;
import mmarkoss.ledger_api.features.statements.dto.StatementEntryRes;
import mmarkoss.ledger_api.features.statements.dto.StatementPageRes;
import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatementHappyPathIT extends StatementITSupport {

    @Test
    @DisplayName("GET /accounts/{id}/statements expõe depósito e transferência em desc e asc")
    void deveLerExtratoComDepositoETransferencia() {
        UUID source = criarConta(HOLDER_SOURCE, DOC_SOURCE);
        UUID destination = criarConta(HOLDER_DESTINATION, DOC_DESTINATION);

        depositar(source, "dep-source", "100.00");
        TransferRes transfer = transferir(source, destination, "trf-1", "30.00");

        StatementPageRes desc = extratoOk(source, "");

        assertThat(desc.order()).isEqualTo("desc");
        assertThat(desc.hasMore()).isFalse();
        assertThat(desc.nextCursor()).isNull();
        assertThat(desc.entries()).hasSize(2);

        StatementEntryRes transferLine = desc.entries().get(0);
        assertThat(transferLine.entryId()).isNotNull();
        assertThat(transferLine.transactionId()).isEqualTo(transfer.transactionId());
        assertThat(transferLine.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(transferLine.direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(transferLine.amount()).isEqualByComparingTo("30");
        assertThat(transferLine.balanceAfter()).isEqualByComparingTo("70");
        assertThat(transferLine.counterpartyId()).isEqualTo(destination);
        assertThat(transferLine.createdAt()).isNotNull();

        StatementEntryRes depositLine = desc.entries().get(1);
        assertThat(depositLine.entryId()).isLessThan(transferLine.entryId());
        assertThat(depositLine.transactionId()).isNotEqualTo(transfer.transactionId());
        assertThat(depositLine.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(depositLine.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(depositLine.amount()).isEqualByComparingTo("100");
        assertThat(depositLine.balanceAfter()).isEqualByComparingTo("100");
        assertThat(depositLine.counterpartyId()).isNull();
        assertThat(depositLine.createdAt()).isNotNull();

        StatementPageRes asc = extratoOk(source, "?order=asc");

        assertThat(asc.order()).isEqualTo("asc");
        assertThat(asc.entries()).extracting(StatementEntryRes::type)
                .containsExactly(TransactionType.DEPOSIT, TransactionType.TRANSFER);

        StatementPageRes destinationStatement = extratoOk(destination, "");

        assertThat(destinationStatement.entries()).hasSize(1);
        StatementEntryRes credit = destinationStatement.entries().getFirst();
        assertThat(credit.transactionId()).isEqualTo(transfer.transactionId());
        assertThat(credit.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(credit.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("30");
        assertThat(credit.balanceAfter()).isEqualByComparingTo("30");
        assertThat(credit.counterpartyId()).isEqualTo(source);
        assertThat(credit.createdAt()).isNotNull();
    }
}
