package mmarkoss.ledger_api.features.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Única porta de mutação de saldo e criação de lançamento. O par
 * {@code Account.credit/debit} + {@code LedgerEntry} nasce na mesma transação e o
 * {@code balanceAfter} é lido da mesma instância de conta que sofreu a mutação.
 */
@Service
public class LedgerPostingService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerPostingService(AccountRepository accountRepository,
                                LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Account requireAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public LedgerEntry credit(UUID transactionId, Account account, BigDecimal amount) {
        account.credit(amount);

        return ledgerEntryRepository.save(LedgerEntry.credit(
                transactionId, account.getId(), amount, account.getBalance()));
    }

    @Transactional
    public LedgerEntry debit(UUID transactionId,
                             Account account,
                             BigDecimal amount,
                             UUID counterpartyId) {
        account.debit(amount);

        return ledgerEntryRepository.save(LedgerEntry.debit(
                transactionId, account.getId(), amount, account.getBalance(), counterpartyId));
    }
}
