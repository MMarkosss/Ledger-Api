-- V1__create_core_tables.sql

-- ============================================================
-- ACCOUNT: o "donos" do dinheiro
-- ============================================================
CREATE TABLE account (
                         id          UUID        PRIMARY KEY DEFAULT uuidv7(),
                         holder_name TEXT        NOT NULL,
                         document    TEXT        NOT NULL UNIQUE,          -- CPF/CNPJ
                         balance     NUMERIC(19,4) NOT NULL DEFAULT 0
                             CHECK (balance >= 0),                 -- invariante no banco
                         version     BIGINT      NOT NULL DEFAULT 0,       -- optimistic locking
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- TRANSACTION: cabeça da operação (1 por transferência/depósito)
-- ============================================================
CREATE TABLE transaction (
                             id              UUID        PRIMARY KEY DEFAULT uuidv7(),
                             type            TEXT        NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER')),
                             amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
                             idempotency_key TEXT        NOT NULL UNIQUE,      -- trava no banco
                             idempotency_payload_hash TEXT NOT NULL,           -- mesma chave, body diferente?
                             status          TEXT        NOT NULL DEFAULT 'COMPLETED'
                                 CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- LEDGER_ENTRY: append-only. O extrato É esta tabela.
-- ============================================================
CREATE TABLE ledger_entry (
                              id              BIGSERIAL   PRIMARY KEY,
                              transaction_id  UUID        NOT NULL REFERENCES transaction(id),
                              account_id      UUID        NOT NULL REFERENCES account(id),
                              direction       CHAR(1)     NOT NULL CHECK (direction IN ('C', 'D')),
                              amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
                              balance_after   NUMERIC(19,4) NOT NULL,           -- snapshot p/ extrato rápido
                              counterparty_id UUID        REFERENCES account(id),
                              created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índices: cada um existe por um motivo
CREATE INDEX idx_entry_account ON ledger_entry(account_id, id DESC);  -- extrato paginado
CREATE INDEX idx_entry_tx      ON ledger_entry(transaction_id);        -- achar o par C/D
CREATE INDEX idx_entry_idem    ON ledger_entry(transaction_id);        -- conciliação