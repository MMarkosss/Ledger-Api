package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.accounts.Account;
import mmarkoss.ledger_api.features.ledger.EntryDirection;
import mmarkoss.ledger_api.features.ledger.LedgerEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferPairingTest {

    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();
    private static final UUID DESTINATION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Pareia débito e crédito com direção, valor, snapshot e contraparte")
    void deveParearDebitoECreditoComSnapshot() {
        Account source = Account.open("Maria Silva", "11111111111");
        source.credit(new BigDecimal("100.00"));

        Account destination = Account.open("João Souza", "22222222222");
        destination.credit(new BigDecimal("20.00"));

        TransferPair pair = TransferPair.post(
                TRANSACTION_ID,
                SOURCE_ID,
                source,
                DESTINATION_ID,
                destination,
                new BigDecimal("30.00"));

        assertThat(source.getBalance()).isEqualByComparingTo("70");
        assertThat(destination.getBalance()).isEqualByComparingTo("50");

        LedgerEntry debit = pair.debit();
        assertThat(debit.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(debit.getAccountId()).isEqualTo(SOURCE_ID);
        assertThat(debit.getDirection()).isEqualTo(EntryDirection.DEBIT);
        assertThat(debit.getAmount()).isEqualByComparingTo("30");
        assertThat(debit.getBalanceAfter()).isEqualByComparingTo("70");
        assertThat(debit.getCounterpartyId()).isEqualTo(DESTINATION_ID);

        LedgerEntry credit = pair.credit();
        assertThat(credit.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(credit.getAccountId()).isEqualTo(DESTINATION_ID);
        assertThat(credit.getDirection()).isEqualTo(EntryDirection.CREDIT);
        assertThat(credit.getAmount()).isEqualByComparingTo("30");
        assertThat(credit.getBalanceAfter()).isEqualByComparingTo("50");
        assertThat(credit.getCounterpartyId()).isEqualTo(SOURCE_ID);
    }
}
