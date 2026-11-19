package mmarkoss.ledger_api.features.accounts;

import mmarkoss.ledger_api.features.accounts.dto.AccPostReq;
import mmarkoss.ledger_api.features.accounts.dto.AccUpdateReq;
import mmarkoss.ledger_api.features.ledger.AccountHasLedgerEntriesException;
import mmarkoss.ledger_api.features.ledger.LedgerConstraintNames;
import mmarkoss.ledger_api.features.ledger.LedgerEntryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accRepo;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accRepo,
                          LedgerEntryRepository ledgerEntryRepository) {
        this.accRepo = accRepo;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Account findByIdOrThrow(UUID id) {
        return accRepo.findById(id).orElseThrow(() ->
                new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Account> listAccs(Pageable pageable) {
        return accRepo.findAll(pageable);
    }

    @Transactional
    public Account createAcc(AccPostReq dto) {
        if (accRepo.existsByDocument(dto.document())) {
            throw new DuplicateDocumentException();
        }

        Account newAcc = Account.open(dto.holderName(), dto.document());

        try {
            return accRepo.saveAndFlush(newAcc);
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintNames.violates(
                    ex, "23505", ConstraintNames.ACCOUNT_DOCUMENT_UNIQUE)) {
                throw new DuplicateDocumentException();
            }
            throw ex;
        }
    }

    @Transactional
    public Account updateAcc(UUID id, AccUpdateReq dto) {
        Account acc = findByIdOrThrow(id);
        acc.rename(dto.holderName());

        return acc;
    }

    @Transactional
    public void deleteAcc(UUID id) {
        Account acc = findByIdOrThrow(id);

        if (ledgerEntryRepository.existsByAccountId(id)) {
            throw new AccountHasLedgerEntriesException(id);
        }

        try {
            accRepo.delete(acc);
            accRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintNames.violates(
                    ex, "23503", LedgerConstraintNames.LEDGER_ENTRY_ACCOUNT_FK)) {
                throw new AccountHasLedgerEntriesException(id);
            }
            throw ex;
        }
    }
}
