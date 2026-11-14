package mmarkoss.ledger_api.features.ledger;

import mmarkoss.ledger_api.features.accounts.Account;
import mmarkoss.ledger_api.features.accounts.AccountNotFoundException;
import mmarkoss.ledger_api.features.accounts.AccountRepository;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class DepositAttemptService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DepositAttemptService(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public DepositResult attempt(UUID accountId, String idempotencyKey, BigDecimal amount) {
        String payloadHash = IdempotencyHash.sha256(
                accountId + "|" + IdempotencyHash.canonicalAmount(amount));

        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), accountId, payloadHash);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        Account.validateAmount(amount);

        UUID transactionId = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        int inserted = transactionRepository.insertIfAbsent(
                transactionId,
                TransactionType.DEPOSIT.name(),
                amount,
                idempotencyKey,
                payloadHash,
                TransactionStatus.COMPLETED.name());

        if (inserted == 0) {
            Transaction winner = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Conflito de idempotência sem transação vencedora"));
            return replay(winner, accountId, payloadHash);
        }

        account.credit(amount);

        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .direction(EntryDirection.CREDIT)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .build();
        ledgerEntryRepository.save(entry);

        return new DepositResult(transactionId, entry.getBalanceAfter(), false);
    }

    private DepositResult replay(Transaction transaction, UUID accountId, String payloadHash) {
        if (!transaction.getIdempotencyPayloadHash().equals(payloadHash)) {
            throw new IdempotencyConflictException();
        }

        LedgerEntry entry = ledgerEntryRepository
                .findByTransactionIdAndAccountId(transaction.getId(), accountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Transação idempotente sem lançamento correspondente"));

        return new DepositResult(transaction.getId(), entry.getBalanceAfter(), true);
    }
}
