package mmarkoss.ledger_api.features.ledger;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DepositService {

    private static final int MAX_ATTEMPTS = 3;

    private final DepositAttemptService attemptService;

    public DepositService(DepositAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    public DepositResult deposit(UUID accountId, String idempotencyKey, BigDecimal amount) {
        OptimisticLockingFailureException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return attemptService.attempt(accountId, idempotencyKey, amount);
            } catch (OptimisticLockingFailureException ex) {
                lastFailure = ex;
            }
        }

        throw lastFailure;
    }
}
