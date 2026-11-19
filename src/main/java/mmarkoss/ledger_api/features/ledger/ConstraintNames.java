package mmarkoss.ledger_api.features.ledger;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public final class ConstraintNames {
    public static final String ACCOUNT_DOCUMENT_UNIQUE = "account_document_key";
    public static final String LEDGER_ENTRY_ACCOUNT_FK = "ledger_entry_account_id_fkey";

    private ConstraintNames() {
    }

    public static boolean violates(DataIntegrityViolationException ex,
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
