package mmarkoss.ledger_api.features.ledger;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Política de deleção de conta no contexto do ledger: uma conta com lançamento não pode
 * ser removida. A FK continua sendo o árbitro; o pré-check só melhora a mensagem comum.
 */
@Service
public class AccountDeletionService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountDeletionService(AccountRepository accountRepository,
                                  LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public void delete(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        if (ledgerEntryRepository.existsByAccountId(id)) {
            throw new AccountHasLedgerEntriesException(id);
        }

        try {
            accountRepository.delete(account);
            accountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintNames.violates(ex, "23503", ConstraintNames.LEDGER_ENTRY_ACCOUNT_FK)) {
                throw new AccountHasLedgerEntriesException(id);
            }
            throw ex;
        }
    }
}
