package mmarkoss.ledger_api.features.accounts;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class ConstraintNames {
    static final String ACCOUNT_DOCUMENT_UNIQUE = "account_document_key";

    private ConstraintNames() {
    }

    static boolean violates(DataIntegrityViolationException ex,
                            String sqlState,
                            String constraint) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && sqlState.equals(violation.getSQLState())
                    && constraint.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
