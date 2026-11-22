# TODO.md — Ledger-Api

> Estado + próximo. Política: `PROCESS.md` · Decisões: `docs/decisions/` · História: `docs/history/`.

**Estado:** nenhum slice ativo — `transfers` encerrado em 2026-11-22. Próximo: extrato.
**Suíte:** 15 unit/contexto / 38 integração / 0 e2e — alvo 70/20/10 (`PROCESS.md`).
O desvio é conhecido; a primeira jornada e2e/BDD nasce no extrato.
**Contexto:** `Account` em `features/accounts`; `features/ledger` é kernel separado
(ADR-007/011). A dependência `accounts -> ledger` é explícita na deleção; depósito e
transferência orquestram conta + lançamento; `balanceAfter` é snapshot do use case, não
invariante. `LedgerEntry` continua append-only (ADR-009); DTOs de conta unificados; ITs
limpam com `TRUNCATE` sem `CASCADE` (ADR-008); `InvalidIdempotencyKeyException` fica em
`shared.web`.
**Próximo foco:** abrir o slice de extrato; D5/D8/D9/D20 ficam no gatilho dele.

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
- [ ] **D20** E2E da jornada de transferência — adiado para o extrato: um smoke agora
      duplicaria `TransferHappyPathIT`/ITs de transferência e, sem extrato, não provaria
      a auditabilidade nem a ordem do ledger. Gatilho: extrato, junto com D8 (ADR-012).

Fechados ou absorvidos: **D4, D6, D12, D14, D16, D18, D19**; **D11** superseded por
R5/ADR-001. Fora da lista (história): **D1, D2, D3, D10, D17**.

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
- `transfers` TR0–TR5 — fechado 2026-11-22 ·
  [`slice-transfers`](docs/history/slice-transfers.md)
- review externa — 2026-11-08 ·
  [`review-externa-2026-11-08`](docs/history/review-externa-2026-11-08.md)

## Contexto

Ledger de pagamentos estilo Pix simplificado: criar conta, depositar, transferir, extrato.
Regras duras: nenhuma transferência processada 2x (idempotência), saldo nunca negativo,
extrato auditável.
Base SQL (`V1`): `account(check balance>=0, version)`, `transaction(idempotency_key UNIQUE)`,
`ledger_entry` append-only (BIGSERIAL). Insumos históricos fora do projeto:
`~/Documentos/ledger-api-insumos/`.
