package mmarkoss.ledger_api.features.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {

    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal BALANCE_AFTER = new BigDecimal("110.00");

    @Test
    void deveCriarCreditoComDirecaoCorreta() {
        LedgerEntry entry = LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, AMOUNT, BALANCE_AFTER);

        assertThat(entry.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(entry.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(entry.getDirection()).isEqualTo(EntryDirection.CREDIT);
        assertThat(entry.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(BALANCE_AFTER);
        assertThat(entry.getCounterpartyId()).isNull();
    }

    @Test
    void deveCriarDebitoComDirecaoEContraparte() {
        UUID counterpartyId = UUID.randomUUID();

        LedgerEntry entry = LedgerEntry.debit(
                TRANSACTION_ID, ACCOUNT_ID, AMOUNT, BALANCE_AFTER, counterpartyId);

        assertThat(entry.getDirection()).isEqualTo(EntryDirection.DEBIT);
        assertThat(entry.getCounterpartyId()).isEqualTo(counterpartyId);
    }

    @Test
    void deveRejeitarEntradaInvalida() {
        assertThatThrownBy(() -> LedgerEntry.credit(
                null, ACCOUNT_ID, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, null, AMOUNT, BALANCE_AFTER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, null, BALANCE_AFTER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, BigDecimal.ZERO, BALANCE_AFTER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, new BigDecimal("-0.01"), BALANCE_AFTER))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, AMOUNT, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerEntry.credit(
                TRANSACTION_ID, ACCOUNT_ID, AMOUNT, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
