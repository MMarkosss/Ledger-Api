# TODO.md — Ledger-Api

> Estado + próximo. Política: `PROCESS.md` · Decisões: `docs/decisions/` · História: `docs/history/`.

**Estado:** slice `transfers` em andamento — TR0–TR1 fechados em 2026-11-21; TR2–TR4
fechados em 2026-11-22; TR5 aberto. E2e/BDD adiados para o extrato (ADR-012, D8/D20).
**Suíte:** 14 unit/contexto / 38 integração / 0 e2e — alvo 70/20/10 (`PROCESS.md`).
O desvio é conhecido; a primeira jornada e2e/BDD nasce no extrato.
**Contexto:** `Account` em `features/accounts`; `features/ledger` é kernel separado
(ADR-007/011). A dependência `accounts -> ledger` é explícita na deleção; o depósito
orquestra conta + lançamento; `balanceAfter` é snapshot do use case, não invariante.
`LedgerEntry` continua append-only (ADR-009); DTOs de conta unificados; ITs limpam com
`TRUNCATE` sem `CASCADE` (ADR-008); `InvalidIdempotencyKeyException` fica em `shared.web`.
**Próximo foco:** TR5 — fechamento do slice, D19 e documentação. O e2e/BDD nasce no
extrato (ADR-012, D8/D20).

## G — gates de publicação

- [ ] **R10** Post do LinkedIn alinhado (UUIDv7 × BIGSERIAL) — README alinhado;
      publicação externa pendente (humano).

## D — débitos abertos

- [ ] **D5** Soft delete de conta — gatilho: extrato.
- [ ] **D7** `updated_at` autoritativo no banco (trigger) — gatilho: auditoria.
- [ ] **D8** BDD da jornada — gatilho: extrato. O e2e da transferência foi adiado para o
      extrato (D20) e não conta como BDD.
- [ ] **D9** Cobertura de timestamps — gatilho: extrato.
- [ ] **D13** Arquitetura de erros curada — `DomainException`, `NoResourceFoundException` →
      404, `traceId`, `include-message/stacktrace=never`, auditoria de mensagens. Gatilho:
      todos os slices encerrados + e2e verde.
- [ ] **D15** PATCH sem semântica parcial — gatilho: demanda real.
- [ ] **D19** Teste unitário do pareamento conta + lançamento — a orquestração de
      `Account.credit/debit` + `LedgerEntry.credit/debit` com `balanceAfter` será repetida
      na transferência; provar direção, valor e snapshot sem HTTP/DB. Gatilho: TR5 da
      transferência.
- [ ] **D20** E2E da jornada de transferência — adiado para o extrato: um smoke agora
      duplicaria `TransferHappyPathIT`/ITs de transferência e, sem extrato, não provaria
      a auditabilidade nem a ordem do ledger. Gatilho: extrato, junto com D8 (ADR-012).

Fechados ou absorvidos: **D4, D6, D12, D14, D16, D18**; **D11** superseded por
R5/ADR-001. Fora da lista (história): **D1, D2, D3, D10, D17**.

## Slice `transfers` — TR0–TR5

Foco: primeira operação com duas contas. Decisão A do ADR-012: lock pessimista
determinístico, sem retry interno; B1/B2 rejeitadas. Sem migração: `V1` já tem `TRANSFER`,
`counterparty_id` e dois lançamentos por transação.

- [x] **TR0** Decisão e contrato — ADR-012 com entradas adversariais A/B1/B2; endpoint
      `POST /transfers`, resposta com os dois saldos, erro de mesma conta em `422` e
      conflito de lock retryable em `409` distinto da idempotência. — pago 2026-11-21.
- [x] **TR1** Caminho feliz — controller/service/DTOs em `features/transfers`; débito na
      origem + crédito no destino + dois `ledger_entry` com `counterparty_id`; lock
      ordenado. Teste: `TransferHappyPathIT`. — pago 2026-11-21.
- [x] **TR2** Erros — mesma conta, origem/destino inexistente, saldo insuficiente, header
      e amount inválidos; rollback sem transação/lançamento. Teste:
      `TransferValidationIT`. — pago 2026-11-22.
- [x] **TR3** Idempotência — `201` novo, `200` replay com os `balance_after` armazenados,
      `409` para payload/tipo diferente, concorrência com a mesma chave. Teste:
      `TransferIdempotencyIT`. — pago 2026-11-22.
- [x] **TR4** Concorrência — chaves diferentes na mesma origem e direções opostas sem
      deadlock/`500`; lock timeout mapeado para `409` retryable. Teste:
      `TransferConcurrencyIT`. — pago 2026-11-22.
- [ ] **TR5** Fechamento — D19 (unitário do pareamento), README/TODO/história e revisão
      de decisões. Teste: `TransferPairingTest`. O e2e/BDD fica no extrato (D8/D20).

Término: TR1–TR5 + suíte integral verde + revisão de decisões — registrado em
`docs/history/slice-transfers.md`; e2e/BDD no extrato (ADR-012, D8/D20).

## Slices encerrados

- **R1–R9** — pacote pré-P11 pago (2026-11-09/11); R10 é gate humano.
- `accounts` — encerrado 2026-11-04 · [`slice-accounts`](docs/history/slice-accounts.md)
- `deposits` P9–P13 — fechado 2026-11-11 · [`slice-deposits`](docs/history/slice-deposits.md)
- `ledger-split` L1–L3 — fechado 2026-11-17 · [`slice-ledger-split`](docs/history/slice-ledger-split.md)
- `test-isolation` TI1–TI4 — fechado 2026-11-17 · [`slice-test-isolation`](docs/history/slice-test-isolation.md)
- `polish` Q1–Q3 — fechado 2026-11-18 · [`slice-polish`](docs/history/slice-polish.md)
- `ledger-context` LC1–LC4 — revertido por ADR-011 em 2026-11-19 ·
  [`slice-ledger-context`](docs/history/slice-ledger-context.md)
- `explicit-ledger-dependency` ED1–ED3 — fechado 2026-11-19 ·
  [`slice-explicit-ledger-dependency`](docs/history/slice-explicit-ledger-dependency.md)
- review externa — 2026-11-08 ·
  [`review-externa-2026-11-08`](docs/history/review-externa-2026-11-08.md)

## Contexto

Ledger de pagamentos estilo Pix simplificado: criar conta, depositar, transferir, extrato.
Regras duras: nenhuma transferência processada 2x (idempotência), saldo nunca negativo,
extrato auditável.
Base SQL (`V1`): `account(check balance>=0, version)`, `transaction(idempotency_key UNIQUE)`,
`ledger_entry` append-only (BIGSERIAL). Insumos históricos fora do projeto:
`~/Documentos/ledger-api-insumos/`.
