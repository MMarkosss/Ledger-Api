package MMarkoss.ledger_api.features.accounts;

import MMarkoss.ledger_api.features.accounts.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AccountRepository extends JpaRepository <Account, UUID> {}
