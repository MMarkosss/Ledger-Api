package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.features.accounts.dto.AccPostReq;
import MMarkoss.ledger_api.features.accounts.dto.AccUpdateReq;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accRepo;

    public AccountService(AccountRepository accRepo) {
        this.accRepo = accRepo;
    }

    @Transactional (readOnly = true)
    public Account findOneByIdAcc(UUID id) {
        return accRepo.findById(id).orElseThrow(() ->
                new AccountNotFoundException(id));
    }

    @Transactional (readOnly = true)
    public Page<Account> listAccs(Pageable pageable) {
        return accRepo.findAll(pageable);
    }

    @Transactional
    public Account createAcc(AccPostReq dto) {
        Account newAcc = Account.builder()
                .holderName(dto.holderName())
                .document(dto.document())
                .build();

        return accRepo.save(newAcc);
    }

    @Transactional
    public Account updateAcc(UUID id, AccUpdateReq dto) {
        Account acc = findOneByIdAcc(id);

        return AccountUpdater.account(acc)
                .holderName(dto.holderName())
                .build();
    }

    @Transactional
    public void deleteAcc (UUID id) {
        Account acc = findOneByIdAcc(id);
        if (accRepo.existsLedgerEntryFor(id)) {
            throw new AccountHasLedgerEntriesException(id);
        }
        accRepo.delete(acc);
    }

}
