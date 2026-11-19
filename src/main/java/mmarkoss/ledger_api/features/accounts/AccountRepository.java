package mmarkoss.ledger_api.features.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    boolean existsByDocument(String document);
}
