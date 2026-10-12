package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.features.accounts.dto.AccPatchReq;
import MMarkoss.ledger_api.features.accounts.dto.AccPostReq;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accRepo;

    public AccountService(AccountRepository accRepo) {
        this.accRepo = accRepo;
    }

    private Account buildAccount(AccPostReq dto) {
        return Account.builder()
                .holderName(dto.holderName())
                .document(dto.document())
                .balance(dto.balance())
                .build();
    }

    @Transactional
    public Account findOneByIdAcc(UUID id) {
        return accRepo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Conta id: " + id + "não foi encontrada"));
    }

    @Transactional
    public Page<Account> listAccs(Pageable pageable) {
        return accRepo.findAll(pageable);
    }

    @Transactional
    public Account createAcc(AccPostReq dto) {
        Account newAcc = buildAccount(dto);

        return accRepo.save(newAcc);
    }

    @Transactional
    public Account updateAcc (AccPostReq dto) {
        Account updatedAcc = buildAccount(dto);

        return accRepo.save(updatedAcc);
    }

    @Transactional
    public Account partialupdate (UUID id, AccPatchReq dto) {
        Account acc = findOneByIdAcc(id);

        return AccountPatchBuilder.account(acc)
                .holderName(dto.holderName())
                .document(dto.document())
                .balance(dto.balance())
                .build();
    }

    @Transactional
    public void deleteAcc (UUID id) {
    Account acc = findOneByIdAcc(id);

    accRepo.delete(acc);
    }

}
