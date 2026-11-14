# TODO.md — Ledger-Api

> Estado + próximo. Política: `PROCESS.md` · Decisões: `docs/decisions/` · História: `docs/history/`.

**Estado:** passo 5 fechado 2026-11-11 · slice `deposits` — P9–P13 fechados · R9 pago ·
R10 gate humano · `accounts` encerrado 2026-11-04 · review externa triada 2026-11-08.
**Pacote pré-P11** (ratificado 2(a) — valor econômico, não bloqueio): **R1–R8** ·
R1–R8 pagos 2026-11-09/11 · R7.2 idioma: **EN** (veredito 2026-11-09).
**Suíte:** 9 unit/contexto / 18 integração / 0 e2e — e2e nasce na transferência · alvo
70/20/10 (`PROCESS.md`). Desvio atual registrado, não declarado convergente.
**Processo:** regra 7 revista + regras 11/12/13 em 2026-11-14.

## R — ações pré-P11 (R1–R8; morrem ao executar)

- [x] **R1** Tradução de constraint no service — `createAcc`: `existsByDocument` + catch com
      `saveAndFlush`; `deleteAcc`: FK → `AccountHasLedgerEntriesException`; SQLState
      (`23505`/`23503`) + nome da constraint; `ConstraintNames` na feature. — pago 2026-11-11.
- [x] **R2** Fallback do advice com log — `log.error("{} {}", method, uri, ex)`; resposta
      estática (interino D13). — pago 2026-11-11.
- [x] **R3** Pin `postgres:18` no `TestcontainersConfiguration` (V1 usa `uuidv7()`; PG18+).
      — pago 2026-11-09.
- [x] **R4** Properties: `ddl-auto=validate` · `open-in-view=false` ·
      `management.endpoints.web.exposure.include=health,info,prometheus`.
      — pago 2026-11-09.
- [x] **R5** Invariantes na `Account` — `credit`/`debit`/`rename` + exceções de domínio;
      deletar operator/updater/`applyDelta`/setters; 8 unitários → `AccountTest`.
      — pago 2026-11-11.
- [x] **R6** `V2__drop_redundant_ledger_index.sql` — `DROP INDEX idx_entry_idem;`.
      — pago 2026-11-09.
- [x] **R7** Renomes — R7.1 pacote `MMarkoss` → `mmarkoss`; R7.2 idioma EN
      (`POST /accounts/{id}/deposits`); R7.3 `AccSummaryRes`/`AccDetailRes` +
      `findByIdOrThrow`. — pago 2026-11-11.
- [x] **R8** `compose.yaml` — `ledger/ledger/ledger`.
      — pago 2026-11-09.

## G — gates de publicação

- [x] **R9** README + CI (GitHub Actions + `mvn verify` + badge). — pago 2026-11-11.
- [ ] **R10** Post do LinkedIn alinhado (UUIDv7 × BIGSERIAL) — README alinhado;
      publicação externa pendente (humano).

## Slice `deposits` — P9–P13

Foco: R9 pago; R10 é gate humano — ver **G**.

Concluídos:
- [x] **P13** Concorrência — optimistic lock + `INSERT ... ON CONFLICT (idempotency_key)
      DO NOTHING` + caminho de replay. — pago 2026-11-11.
- [x] **P12** Erros do depósito — exceções de domínio (422, decisão do P12); header
      `Idempotency-Key` ausente → 400; valor inválido + testes. — pago 2026-11-11.
- [x] **P11** `POST /accounts/{id}/deposits` — validação + `Account.credit()` + transação
      atômica (transaction + entry) + `{transactionId, balance}`; replay conforme P9.
      — pago 2026-11-11.
- [x] **P10** `Transaction` + `LedgerEntry` em `features/ledger` — `id Long/IDENTITY`
      (BIGSERIAL), referências UUID cru, `EntryDirection` (enum + converter `C`/`D`),
      `@CreationTimestamp`. Estado intermediário: sem endpoint até P11 (D12).
      — pago 2026-11-11.
- [x] **P9** Contrato de idempotência — `Idempotency-Key` obrigatório; SHA-256
      (`accountId|amount` normalizado); 200 replay / 409 hash diferente / 201 novo;
      `status` sempre `COMPLETED`; `PROCESSING`/`FAILED` não expostos.

Término: P9–P13 + rodada de testes do ciclo (escopo definido no P11) verde + debate de
arquitetura — registrado em `docs/history/slice-deposits.md`.

## D — débitos abertos

- [x] **D4** Blindagem total da entidade — normalização do `holderName`; **fechado com R5 verde**.
- [ ] **D5** Soft delete de conta — gatilho: extrato.
- [ ] **D6** `existsLedgerEntryFor` → `LedgerEntryRepository.existsByAccountId` — gatilho:
      extrato/transfer.
- [ ] **D7** `updated_at` autoritativo no banco (trigger) — gatilho: auditoria.
- [ ] **D8** BDD da jornada — gatilho: transfer.
- [ ] **D9** Cobertura de timestamps — gatilho: extrato.
- [x] **D12** Estado intermediário do P10 — fechado no P11 (contrato cobre o mapeamento).
- [ ] **D13** Arquitetura de erros curada — `DomainException`, `NoResourceFoundException` →
      404, `traceId`, `include-message/stacktrace=never`, auditoria de mensagens. Gatilho:
      todos os slices encerrados + e2e verde.
- [x] **D14** ADR/README do dono do ID — pago por R9/README (UUIDv7 na app; default do
      banco como piso) · ADR-002 existente.
- [ ] **D15** PATCH sem semântica parcial — gatilho: demanda real.
- [x] **D16** README das decisões — pago por R9/README (FK como árbitro, BIGSERIAL ×
      UUIDv7, idempotência).
- [ ] **D18 (discussão)** Ordenação explícita de testes na mesma classe com `@Order`
      (JUnit 5.8+) — ideia ligada ao item 5 congelado (AccountContractIT/testes), não ao
      prompt atual. Não implementar agora. Gatilho: **item 5 descongelado**.

Fora da lista (história): **D1, D2, D3, D10** (pagos no slice) · **D11** (superseded por
R5/ADR-001; reabre se R5 for revertido) · **D17** (pago por este split em 2026-11-09).

## Contexto

Ledger de pagamentos estilo Pix simplificado: criar conta, depositar, transferir, extrato.
Regras duras: nenhuma transferência processada 2x (idempotência), saldo nunca negativo,
extrato auditável.
Base SQL (`V1`): `account(check balance>=0, version)`, `transaction(idempotency_key UNIQUE)`,
`ledger_entry` append-only (BIGSERIAL). Insumos históricos fora do projeto:
`~/Documentos/ledger-api-insumos/`.
