# ADR-003 — Tradução de constraint no service

**Status:** accepted — execução R1; interino R2; arquitetura curada = D13
**Data:** 2026-11-09
**Origem:** A10 / A5 / R1

> Nota ADR-011 (2026-11-19): a tradução de documento e de FK voltou para `AccountService`.
> `ConstraintNames` de conta fica em `features/accounts`; o nome da FK fica em
> `LedgerConstraintNames`, no ledger. `AccountHasLedgerEntriesException` também fica no
> ledger; a FK continua sendo o árbitro.

## Contexto

O advice mapeava `DataIntegrityViolationException` por tipo com mensagem fixa: no DELETE,
violação de FK virava "Documento já cadastrado" (mentira); no CHECK, mentiria "Documento"
para saldo. O advice não tem contexto semântico para saber qual constraint violou.

## Decisão

O service (quem tem contexto) traduz a violação por constraint: `existsByDocument` como
pré-check amigável + `catch` com `saveAndFlush` dentro do try; detecção por SQLState
(`23505` único × `23503` FK) + nome da constraint; `ConstraintNames` na feature.
Nota de implementação (2026-11-11): a leitura é feita pela `ConstraintViolationException`
do Hibernate, que expõe SQLState e nome da constraint; o driver PostgreSQL fica em
`runtime` e não é dependência de compilação. `createAcc` → `DuplicateDocumentException`; `deleteAcc` → FK
`ledger_entry_account_id_fkey` → `AccountHasLedgerEntriesException`. O advice perde o
`handleConflict` inteiro; `DataIntegrityViolationException` não traduzida → fallback.
Após o catch a transação está condenada: traduzir e lançar.

## Alternativas rejeitadas (steelman)

- **Switch de exceção no advice** — mente por dispatch de tipo (FK vira "Documento").
- **Ecoar `getMessage()`** — vaza SQL/schema/dado.
- **Pré-check sozinho** — TOCTOU; o catch é a rede.

## Consequências

- Fallback loga `method` + URI + stack (R2) e responde mensagem estática; D13 cura o
  contrato `DomainException`, `NoResourceFoundException` → 404 e `traceId`.
- Violação de CHECK que chega ao banco é bug de camada superior → 500 honesto + log,
  nunca 422.

## Gatilho de reabertura

Quando D13 (arquitetura de erros curada) for implementada, ou se o contrato de exceções
mudar.
