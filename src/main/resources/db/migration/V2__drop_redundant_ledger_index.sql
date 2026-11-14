-- R6: idx_entry_idem era duplicado de idx_entry_tx (ambos em transaction_id).
-- V1 é imutável; a correção entra como V2.
DROP INDEX idx_entry_idem;
