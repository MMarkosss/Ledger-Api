# ADR-004 — BIGSERIAL no `ledger_entry`

**Status:** accepted (P10)
**Data:** 2026-11-09
**Origem:** P10 + A20/R10

## Contexto

O `ledger_entry` é append-only, lido por `(account_id, id DESC)` para extrato. O `account`
usa UUIDv7; o `transaction` já tem `idempotency_key UNIQUE`.

## Decisão

`ledger_entry.id` é `Long`/`IDENTITY` (BIGSERIAL): ordenação estrita + índice
`(account_id, id DESC)` + read-back valem mais que batching. Referências a conta/transação
por UUID cru (sem `@ManyToOne`).

## Alternativas rejeitadas (steelman)

- **UUIDv7 na entry** — perde ordenação intra-ms garantida; precisaria de `created_at`
  como chave do extrato.
- **Batching na tabela mais quente** — a claim "UUIDv7 habilita batching" do post do
  LinkedIn é falsa até R10 alinhar.

## Consequências

- Batching desabilitado no `ledger_entry` por design; read-back por id.
- R10 é gate de publicação, não de código.

## Gatilho de reabertura

Se batching no `ledger_entry` virar requisito real.
