package MMarkoss.ledger_api.features.accounts;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AccountRepository extends JpaRepository <Account, UUID> {
    @Query(value = "select exists(select 1 from ledger_entry e where e.account_id = :id)", nativeQuery = true)
    boolean existsLedgerEntryFor(@Param("id") UUID id);
}
