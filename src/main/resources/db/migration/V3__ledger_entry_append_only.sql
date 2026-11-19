-- ADR-009: ledger_entry é append-only também no banco.
-- UPDATE/DELETE de linha são rejeitados; correções viram novo lançamento.
CREATE OR REPLACE FUNCTION reject_ledger_entry_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entry is append-only: % is not allowed', TG_OP
        USING ERRCODE = '23514';
END;
$$;

DROP TRIGGER IF EXISTS trg_ledger_entry_append_only ON ledger_entry;

CREATE TRIGGER trg_ledger_entry_append_only
    BEFORE UPDATE OR DELETE ON ledger_entry
    FOR EACH ROW
    EXECUTE FUNCTION reject_ledger_entry_mutation();
