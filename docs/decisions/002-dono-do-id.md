# ADR-002 — Dono do ID

**Status:** accepted (D14 registra no README; R10 antes de publicar)
**Data:** 2026-11-09
**Origem:** A21 / D14

## Contexto

`account.id` usa `@UuidGenerator(VERSION_7)` (app-side) e a V1 tem `DEFAULT uuidv7()`
(banco). A decisão existia só no debate; o DEFAULT é código morto para writes da app
(o INSERT sempre leva id) e piso para writers não-app.

## Decisão

`@UuidGenerator(VERSION_7)` é o dono de todo write da app. `DEFAULT uuidv7()` é piso para
writers não-app (psql, script, serviço futuro): um INSERT leva id OU dispara o DEFAULT,
nunca ambos. A ordenação prometida do sistema mora no BIGSERIAL do `ledger_entry`, não no
v7 Java (ordenação intra-ms não garantida).

## Alternativas rejeitadas (steelman)

- **Gerar tudo no banco** — perde o regime app-side e o batching no `account`.
- **`@GeneratedValue(IDENTITY)` sobre coluna `DEFAULT uuidv7()`** — experimento opcional
  (5 min) para documentar o regime banco-dono; não é a decisão.

## Consequências

- README/D14 e R10 documentam; entrevista/publicação não reabrem o litígio.
- Ordenação estrita do extrato vem do `ledger_entry.id`, não do UUID.

## Gatilho de reabertura

Mudança de banco ou escrita multi-serviço no `account`.
