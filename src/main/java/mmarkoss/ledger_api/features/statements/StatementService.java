package mmarkoss.ledger_api.features.statements;

import mmarkoss.ledger_api.features.accounts.AccountNotFoundException;
import mmarkoss.ledger_api.features.accounts.AccountRepository;
import mmarkoss.ledger_api.features.ledger.LedgerEntry;
import mmarkoss.ledger_api.features.ledger.LedgerEntryRepository;
import mmarkoss.ledger_api.features.ledger.Transaction;
import mmarkoss.ledger_api.features.ledger.TransactionRepository;
import mmarkoss.ledger_api.features.ledger.TransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatementService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;

    public StatementService(AccountRepository accountRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public StatementPage getStatement(UUID accountId, StatementQuery query) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        Pageable pageRequest = PageRequest.of(0, query.limit());
        Slice<LedgerEntry> slice = loadPage(accountId, query, pageRequest);
        List<LedgerEntry> entries = slice.getContent();

        Map<UUID, TransactionType> types = transactionRepository
                .findAllById(entries.stream()
                        .map(LedgerEntry::getTransactionId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Transaction::getId, Transaction::getType));

        List<StatementEntry> statementEntries = entries.stream()
                .map(entry -> toEntry(entry, types))
                .toList();

        Long nextCursor = slice.hasNext() && !entries.isEmpty()
                ? entries.getLast().getId()
                : null;

        return new StatementPage(query.order(), statementEntries, nextCursor, slice.hasNext());
    }

    private Slice<LedgerEntry> loadPage(UUID accountId,
                                        StatementQuery query,
                                        Pageable pageRequest) {
        return switch (query.order()) {
            case DESC -> query.cursor() == null
                    ? ledgerEntryRepository.findByAccountIdOrderByIdDesc(accountId, pageRequest)
                    : ledgerEntryRepository.findByAccountIdAndIdLessThanOrderByIdDesc(
                            accountId, query.cursor(), pageRequest);
            case ASC -> query.cursor() == null
                    ? ledgerEntryRepository.findByAccountIdOrderByIdAsc(accountId, pageRequest)
                    : ledgerEntryRepository.findByAccountIdAndIdGreaterThanOrderByIdAsc(
                            accountId, query.cursor(), pageRequest);
        };
    }

    private StatementEntry toEntry(LedgerEntry entry, Map<UUID, TransactionType> types) {
        TransactionType type = types.get(entry.getTransactionId());
        if (type == null) {
            throw new IllegalStateException(
                    "Lançamento sem transação correspondente: " + entry.getId());
        }

        return new StatementEntry(
                entry.getId(),
                entry.getTransactionId(),
                type,
                entry.getDirection(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getCounterpartyId(),
                entry.getCreatedAt());
    }
}
