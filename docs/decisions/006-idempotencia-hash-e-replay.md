# ADR-006 — Idempotência: hash + replay

**Status:** accepted (P9; execução P11/P13)
**Data:** 2026-11-09
**Origem:** P9/P13 (S1)

> Nota ADR-011 (2026-11-19): `IdempotencyHash` continua no ledger; o payload é específico
> da operação (`accountId|amount` no depósito, `source|destination|amount` na futura
> transferência). `InvalidIdempotencyKeyException` permanece em `shared.web`; a borda HTTP
> valida o header, o domínio cuida de hash/replay.

## Contexto

Depósito/transferência não podem processar 2x. O `transaction.idempotency_key` é UNIQUE
global; o `ledger_entry.balance_after` guarda o saldo do momento.

## Decisão

`Idempotency-Key` obrigatório. Hash SHA-256(`accountId|amount` normalizado com
`stripTrailingZeros().toPlainString()`). Mesma chave + mesmo hash → **200** replay
(resposta do `ledger_entry.balance_after` armazenado, não do saldo atual); hash diferente →
**409**; novo → **201**. `status` sempre `COMPLETED` no fluxo síncrono;
`PROCESSING`/`FAILED` reservados ao assíncrono e não expostos. Concorrência: `INSERT ...
ON CONFLICT (idempotency_key) DO NOTHING` + caminho de replay (P13).

## Alternativas rejeitadas (steelman)

- **Estados `PROCESSING`/`FAILED` expostos** — no síncrono nunca commitam; seriam mentira.
- **Chave por conta** — a chave é global; o hash já inclui a conta.
- **Replay com o saldo atual** — infiel ao momento original; usar `balance_after`.

## Consequências

- P11/P13; o replay é idempotente por construção.
- Teste de contrato cobre 201/200/409.

## Gatilho de reabertura

Fluxo assíncrono real.
