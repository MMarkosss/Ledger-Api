package mmarkoss.ledger_api.features.transfers;

import mmarkoss.ledger_api.features.accounts.Account;
import mmarkoss.ledger_api.features.accounts.AccountNotFoundException;
import mmarkoss.ledger_api.features.accounts.AccountRepository;
import mmarkoss.ledger_api.features.ledger.IdempotencyConflictException;
import mmarkoss.ledger_api.features.ledger.IdempotencyHash;
import mmarkoss.ledger_api.features.ledger.LedgerEntry;
import mmarkoss.ledger_api.features.ledger.LedgerEntryRepository;
import mmarkoss.ledger_api.features.ledger.Transaction;
import mmarkoss.ledger_api.features.ledger.TransactionRepository;
import mmarkoss.ledger_api.features.ledger.TransactionStatus;
import mmarkoss.ledger_api.features.ledger.TransactionType;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transaction script da transferência. As duas contas são carregadas em ordem de UUID
 * com lock pessimista, o que evita o ciclo de locks entre A→B e B→A. Não há retry
 * interno: o lock timeout é tratado na borda como conflito retryable.
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public TransferResult transfer(UUID sourceAccountId,
                                   UUID destinationAccountId,
                                   String idempotencyKey,
                                   BigDecimal amount) {
        if (Objects.equals(sourceAccountId, destinationAccountId)) {
            throw new SameAccountTransferException();
        }

        Account.validateAmount(amount);

        String payloadHash = IdempotencyHash.sha256(
                sourceAccountId + "|" + destinationAccountId + "|"
                        + IdempotencyHash.canonicalAmount(amount));

        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), sourceAccountId, destinationAccountId, payloadHash);
        }

        UUID transactionId = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        int inserted = transactionRepository.insertIfAbsent(
                transactionId,
                TransactionType.TRANSFER.name(),
                amount,
                idempotencyKey,
                payloadHash,
                TransactionStatus.COMPLETED.name());

        if (inserted == 0) {
            Transaction winner = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Conflito de idempotência sem transação vencedora"));
            return replay(winner, sourceAccountId, destinationAccountId, payloadHash);
        }

        Map<UUID, Account> accounts = accountRepository
                .findAllByIdInForUpdate(List.of(sourceAccountId, destinationAccountId))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Account source = accounts.get(sourceAccountId);
        if (source == null) {
            throw new AccountNotFoundException(sourceAccountId);
        }

        Account destination = accounts.get(destinationAccountId);
        if (destination == null) {
            throw new AccountNotFoundException(destinationAccountId);
        }

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

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        return new TransferResult(
                transactionId,
                debit.getBalanceAfter(),
                credit.getBalanceAfter(),
                false);
    }

    private TransferResult replay(Transaction transaction,
                                  UUID sourceAccountId,
                                  UUID destinationAccountId,
                                  String payloadHash) {
        if (transaction.getType() != TransactionType.TRANSFER
                || !transaction.getIdempotencyPayloadHash().equals(payloadHash)) {
            throw new IdempotencyConflictException();
        }

        LedgerEntry debit = ledgerEntryRepository
                .findByTransactionIdAndAccountId(transaction.getId(), sourceAccountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Transferência idempotente sem débito correspondente"));
        LedgerEntry credit = ledgerEntryRepository
                .findByTransactionIdAndAccountId(transaction.getId(), destinationAccountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Transferência idempotente sem crédito correspondente"));

        return new TransferResult(
                transaction.getId(),
                debit.getBalanceAfter(),
                credit.getBalanceAfter(),
                true);
    }
}
