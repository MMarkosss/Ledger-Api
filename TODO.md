# TODO.md — Ledger-Api

> Estado + próximo. Política: `PROCESS.md` · Decisões: `docs/decisions/` · História: `docs/history/`.

**Estado:** nenhum slice ativo — `statements` encerrado em 2026-11-24; passo 6 concluído em
2026-11-27 com D22, C1 e D13/D23. Próximo: passo 7 — envelope.
**Suíte:** 17 unit/contexto / 51 integração / 1 e2e — alvo 70/20/10 (`PROCESS.md`).
O desvio é conhecido; a primeira jornada e2e/BDD está no extrato.
**Contexto:** `Account` em `features/accounts`; `features/ledger` é kernel separado
(ADR-007/011). A dependência `accounts -> ledger` é explícita na deleção; depósito e
transferência orquestram conta + lançamento; `balanceAfter` é snapshot do use case, não
invariante. `LedgerEntry` continua append-only (ADR-009); DTOs de conta unificados; ITs
limpam com `TRUNCATE` sem `CASCADE` (ADR-008); `InvalidIdempotencyKeyException` fica em
`shared.web`. O extrato lê o kernel por cursor `id` com `desc` default e `asc` para
auditoria (ADR-013); soft delete foi adiado (ADR-014). Erros de feature usam
`DomainException` + `ProblemDetail`; `TraceIdFilter` propaga `X-Request-Id`/`traceId` e o
fallback é estático (ADR-015).
**Próximo foco:** passo 7 do roteiro — envelope: CI (`verify` no push/PR), `prometheus` e
README alinhado já existem; resta a revisão final do envelope e o gate humano R10.
D13/D23 fechados em 2026-11-27.

## G — gates de publicação

- [ ] **R10** Post do LinkedIn alinhado (UUIDv7 × BIGSERIAL) — README alinhado;
      publicação externa pendente (humano).

## D — débitos abertos

- [ ] **D5** Soft delete de conta — adiado por ADR-014; gatilho: produto/auditoria
      precisar desativar ou continuar lendo uma conta com histórico.
- [ ] **D7** `updated_at` autoritativo no banco (trigger) — gatilho: auditoria.
- [ ] **D15** PATCH sem semântica parcial — gatilho: demanda real.
- [ ] **D21** Camada de enriquecimento do extrato — nome/descrição da contraparte para
      humano/UI, testes unitários e apresentação. Gatilho subjetivo: conclusão do projeto
      core; não bloqueia o fechamento funcional atual.

Fechados ou absorvidos: **D4, D6, D8, D9, D12, D13, D14, D16, D18, D19, D20, D22, D23**;
**D11** superseded por R5/ADR-001. Fora da lista (história): **D1, D2, D3, D10, D17**.

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
- `statements` ST0–ST5 — fechado 2026-11-24 ·
  [`slice-statements`](docs/history/slice-statements.md)
- `errors` D13/D23 — fechado 2026-11-27 ·
  [`slice-errors`](docs/history/slice-errors.md)
- review externa — 2026-11-08 ·
  [`review-externa-2026-11-08`](docs/history/review-externa-2026-11-08.md)

## Contexto

Ledger de pagamentos estilo Pix simplificado: criar conta, depositar, transferir, extrato.
Regras duras: nenhuma transferência processada 2x (idempotência), saldo nunca negativo,
extrato auditável.
Base SQL (`V1`): `account(check balance>=0, version)`, `transaction(idempotency_key UNIQUE)`,
`ledger_entry` append-only (BIGSERIAL). Insumos históricos fora do projeto:
`~/Documentos/ledger-api-insumos/`.
