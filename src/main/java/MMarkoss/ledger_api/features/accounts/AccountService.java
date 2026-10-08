package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.features.accounts.dto.AccPostReqDTO;

import jakarta.persistence.EntityNotFoundException;
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

    private Account buildAccount(AccPostReqDTO dto) {
        return Account.builder()
                .holderName(dto.holderName())
                .document(dto.documents())
                .balance(dto.balance())
                .build();
    }

    public Account findOneByIdAcc(UUID id) {
        return accRepo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Conta id: " + id + "não foi encontrada"));
    }

    public Page<Account> listAccs(Pageable pageable) {
        return accRepo.findAll(pageable);
    }

    public Account createAcc(AccPostReqDTO dto) {
        Account newAcc = buildAccount(dto);
        return accRepo.save(newAcc);
    }

    public Account updateAcc (AccPostReqDTO dto) {
        Account updatedAcc = buildAccount(dto);
        return accRepo.save(updatedAcc);
    }
}
