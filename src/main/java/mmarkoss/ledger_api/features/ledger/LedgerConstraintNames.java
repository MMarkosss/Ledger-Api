package mmarkoss.ledger_api.features.ledger;

/**
 * Nomes de constraints que pertencem ao ledger. A tradução da violação fica no service
 * que tem contexto; aqui fica apenas o nome que o ledger conhece.
 */
public final class LedgerConstraintNames {
    public static final String LEDGER_ENTRY_ACCOUNT_FK = "ledger_entry_account_id_fkey";

    private LedgerConstraintNames() {
    }
}
