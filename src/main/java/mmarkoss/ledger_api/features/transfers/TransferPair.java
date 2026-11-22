package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.accounts.Account;
import mmarkoss.ledger_api.features.ledger.LedgerEntry;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pareamento local da transferência: muta as duas contas e cria as duas pernas do ledger
 * com o snapshot posterior. Não é um posting service central; é um value object puro da
 * fatia de transferência, sem repositório nem Spring.
 */
record TransferPair(LedgerEntry debit, LedgerEntry credit) {

    static TransferPair post(UUID transactionId,
                             UUID sourceAccountId,
                             Account source,
                             UUID destinationAccountId,
                             Account destination,
                             BigDecimal amount) {
        source.debit(amount);
        destination.credit(amount);

        LedgerEntry debit = LedgerEntry.debit(
                transactionId,
                sourceAccountId,
                amount,
                source.getBalance(),
                destinationAccountId);
        LedgerEntry credit = LedgerEntry.credit(
                transactionId,
                destinationAccountId,
                amount,
                destination.getBalance(),
                sourceAccountId);

        return new TransferPair(debit, credit);
    }
}
