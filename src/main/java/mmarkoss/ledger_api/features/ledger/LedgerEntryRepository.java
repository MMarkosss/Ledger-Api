package mmarkoss.ledger_api.features.ledger;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Expõe apenas inserção e leitura. Não há delete/deleteAll: o ledger é append-only
 * também na API do repositório.
 */
public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

    LedgerEntry save(LedgerEntry entry);

    Optional<LedgerEntry> findByTransactionIdAndAccountId(UUID transactionId, UUID accountId);

    boolean existsByAccountId(UUID accountId);

    Slice<LedgerEntry> findByAccountIdOrderByIdDesc(UUID accountId, Pageable pageable);

    Slice<LedgerEntry> findByAccountIdAndIdLessThanOrderByIdDesc(UUID accountId,
                                                                 Long id,
                                                                 Pageable pageable);

    Slice<LedgerEntry> findByAccountIdOrderByIdAsc(UUID accountId, Pageable pageable);

    Slice<LedgerEntry> findByAccountIdAndIdGreaterThanOrderByIdAsc(UUID accountId,
                                                                   Long id,
                                                                   Pageable pageable);
}
