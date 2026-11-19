# TODO.md — Ledger-Api

> Estado + próximo. Política: `PROCESS.md` · Decisões: `docs/decisions/` · História: `docs/history/`.

**Estado:** passo 5 fechado 2026-11-11 · slice `deposits` — P9–P13 fechados · R9 pago ·
R10 gate humano · `accounts` encerrado 2026-11-04 · review externa triada 2026-11-08 ·
slice `polish` Q1–Q3 fechado 2026-11-18 · slice `ledger-context` LC1–LC4 revertido
2026-11-19 · slice `explicit-ledger-dependency` ED1–ED3 fechado 2026-11-19.
**Pacote pré-P11** (ratificado 2(a) — valor econômico, não bloqueio): **R1–R8** ·
R1–R8 pagos 2026-11-09/11 · R7.2 idioma: **EN** (veredito 2026-11-09).
**Suíte:** 14 unit/contexto / 21 integração / 0 e2e — e2e nasce na transferência · alvo
70/20/10 (`PROCESS.md`). Desvio atual registrado, não declarado convergente; ED manteve a
prova de `balance_after == account.balance` no depósito e os 2 ITs de deleção.
**Processo:** regra 7 revista + regras 11/12/13 em 2026-11-14.
**Split do ledger:** L1/L2 pagos 2026-11-14 · L3 fechado 2026-11-17 · slice encerrado.
**Testes:** item 4/5 encerrados 2026-11-17 · slice `test-isolation` TI1–TI4 fechado ·
D18 (`@Order`) descartado — a ordem adversarial passou com limpeza centralizada; Q2 trocou
para `TRUNCATE` sem `CASCADE`.
**Polimento:** `LedgerEntry` append-only no Java/ORM e no banco (ADR-009); DTOs de conta
unificados; limpeza dos ITs agora é `TRUNCATE` sem `CASCADE` (ADR-008 atualizado).
**Contexto:** `Account` voltou para `features/accounts`; o ledger é kernel separado
(ADR-007/011). A dependência `accounts -> ledger` é explícita na deleção; o depósito
orquestra conta + lançamento; `balanceAfter` é snapshot do use case, não invariante.
D6 pago com `LedgerEntryRepository.existsByAccountId`.
**Próximo foco:** transfers — fatiar antes de codar; e2e nasce na transferência e fecha
com o extrato.

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

## Slice `ledger-split` — L1–L3

Foco: separar o kernel compartilhado em `features/ledger` das fatias verticais. A
transferência e o extrato continuam fora deste slice.

- [x] **L1** Extrair `IdempotencyHash` para o kernel — canonicalização + SHA-256;
      `DepositAttemptService` usa o kernel; unitário de normalização + ITs de depósito.
      — pago 2026-11-14.
- [x] **L2** Mover depósito para `features/deposits` — controller/services/DTOs/resultado
      e testes acompanham o pacote; apenas mudança de pacote, sem fatiar/renomear testes;
      suíte integral `./mvnw clean verify` verde. — pago 2026-11-14.
- [x] **L3** Fechar separação — `./mvnw clean verify`; ADR-007; história do slice; mapa
      de decisões (regra 13). — pago 2026-11-17.

Término: L1–L3 + suíte integral verde + ADR-007 + revisão de decisões — registrado em
`docs/history/slice-ledger-split.md`.

## Slice `test-isolation` — TI1–TI4

Foco: isolar o banco entre ITs e fatiar os testes por comportamento, sem trocar a camada
de prova (`@SpringBootTest` + Postgres real continua necessário para o contrato).

- [x] **TI1** Base de isolamento — `IntegrationTestSupport` centraliza
      `@SpringBootTest(RANDOM_PORT)`, `@AutoConfigureRestTestClient`, Testcontainers,
      `RestTestClient` e `JdbcTemplate`; limpeza antes de cada teste em ordem FK
      (`ledger_entry → transaction → account`); ITs existentes migrados; ordem adversarial
      verde. A limpeza foi trocada por `TRUNCATE` no Q2. — pago 2026-11-17.
- [x] **TI2** Fatiar depósitos — `DepositHappyPathIT` (1), `DepositIdempotencyIT` (2),
      `DepositValidationIT` (5); `DepositConcurrencyIT` (2) permanece; helpers em
      `DepositITSupport`. — pago 2026-11-17.
- [x] **TI3** Fatiar contas — `AccountCreationIT` (3), `AccountReadIT` (1),
      `AccountUpdateIT` (3), `AccountErrorIT` (1); helpers em `AccountITSupport`.
      — pago 2026-11-17.
- [x] **TI4** Fechar isolamento — `./mvnw clean verify` verde (29 testes: 11/18/0);
      ordem adversarial depósitos → contas verde (28/28); ADR-008; história; mapa e TODO
      sincronizados. — pago 2026-11-17.

Término: TI1–TI4 + suíte integral verde + ordem adversarial + ADR-008 — registrado em
`docs/history/slice-test-isolation.md`.

## Slice `polish` — Q1–Q3

Foco: fechar a criação arbitrária de lançamento no ledger e remover o DTO duplicado de
conta, sem mudar o contrato HTTP.

- [x] **Q1** Fechar a construção do `LedgerEntry` — remover `@Builder` e construtor público;
      factories `credit`/`debit` com validação; `@Immutable`; repositório com apenas
      `save`/leitura; `DepositAttemptService` usa a factory; `LedgerEntryTest`.
      — pago 2026-11-18.
- [x] **Q2** Append-only no banco — `V3__ledger_entry_append_only.sql` com trigger
      `BEFORE UPDATE OR DELETE`; `IntegrationTestSupport` limpa com
      `truncate table ledger_entry, transaction, account restart identity` (sem `CASCADE`);
      `LedgerEntryAppendOnlyIT`; ADR-009 e ADR-008 atualizado. — pago 2026-11-18.
- [x] **Q3** Unificar DTOs de conta — `AccPostRes` removido; `POST` e `GET` usam
      `AccDetailRes`; supports de IT atualizados; suíte integral 33 (14/19/0).
      — pago 2026-11-18.

Término: Q1–Q3 + suíte integral verde + ADR-009 + revisão de decisões — registrado em
`docs/history/slice-polish.md`.

## Slice `ledger-context` — LC1–LC4 (histórico; revertido)

> Revertido por ADR-011 em 2026-11-19. `Account` voltou para `features/accounts`;
> `LedgerPostingService` e `AccountDeletionService` foram removidos; a opção A
> (dependência explícita) foi adotada. Os itens abaixo ficam como história.

Foco: reconhecer que conta e ledger são o mesmo bounded context, tornar a dependência
explícita e fechar a escrita de saldo/lançamento em um único posting service. Transfers
ficam congelados até o fim deste slice.

- [x] **LC1** Decisão e mapa — ADR-010; ADR-007 marcado `superseded`; ADR-009 emendado;
      mapa de dependências e `docs/decisions/README.md` sincronizados. Sem código.
      — pago 2026-11-19.
- [x] **LC2** Posting service e ownership — `Account`/repositório/exceções movidos para
      `features/ledger`; `Account.credit/debit` e factories de `LedgerEntry` package-private;
      `LedgerPostingService` criado; depósito usa o posting service; `DepositHappyPathIT`
      prova `balance_after == account.balance`. — pago 2026-11-19.
- [x] **LC3** Deleção no ledger — `AccountDeletionService` + `existsByAccountId`; D6 pago;
      `AccountDeletionIT` cobre `204` sem lançamento e `409` com lançamento. — pago
      2026-11-19.
- [x] **LC4** Limpeza de fronteira — `InvalidIdempotencyKeyException` movido para
      `shared.web`; canonicalização por operação documentada; suíte integral 35 (14/21/0);
      README/TODO/ADR/história sincronizados. — pago 2026-11-19.

Término: LC1–LC4 + `./mvnw clean verify` verde + ADR-010 + revisão de decisões —
registrado em `docs/history/slice-ledger-context.md`.

## Slice `explicit-ledger-dependency` — ED1–ED3

Foco: reverter a conta para `features/accounts` sem perder Q1–Q3, e tornar explícita a
dependência `accounts -> ledger` na deleção. ADR-011 substitui o ADR-010.

- [x] **ED1** Mover `Account`, `AccountRepository` e exceções de domínio de conta de volta
      para `features/accounts`; remover `LedgerPostingService` e `AccountDeletionService`;
      manter `Account.open`, Q1/Q2 append-only e Q3 DTO único.
      — pago 2026-11-19.
- [x] **ED2** Dependência explícita — `AccountService` usa
      `LedgerEntryRepository.existsByAccountId`, `LedgerConstraintNames` e
      `AccountHasLedgerEntriesException`; `DepositAttemptService` orquestra conta + ledger;
      `AccountTest` volta para `features/accounts`; `AccountDeletionIT` mantido.
      — pago 2026-11-19.
- [x] **ED3** Documentação e rodada — ADR-011; ADR-010 `superseded`; ADR-007/009/001/003/005/006
      sincronizados; README/TODO/história; `./mvnw clean verify` verde 35 (14/21/0).
      — pago 2026-11-19.

Término: ED1–ED3 + suíte integral verde + ADR-011 + revisão de decisões — registrado em
`docs/history/slice-explicit-ledger-dependency.md`.

## D — débitos abertos

- [x] **D4** Blindagem total da entidade — normalização do `holderName`; **fechado com R5 verde**.
- [ ] **D5** Soft delete de conta — gatilho: extrato.
- [x] **D6** `existsLedgerEntryFor` → `LedgerEntryRepository.existsByAccountId` — pago por
      LC3 e preservado no ED2; `AccountService` depende explicitamente do repositório do
      ledger. Gatilho original (extrato/transfer) antecipado pela revisão de contexto.
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
- [x] **D18 (discussão)** Ordenação explícita de testes na mesma classe com `@Order`
      (JUnit 5.8+) — encerrado sem implementação: a dependência era de estado no banco, e
      a limpeza FK no `IntegrationTestSupport` torna a ordem irrelevante. `@Order` só
      esconderia o sintoma. Gatilho original (item 5) cumprido em TI1.

Fora da lista (história): **D1, D2, D3, D10** (pagos no slice) · **D11** (superseded por
R5/ADR-001; reabre se R5 for revertido) · **D17** (pago por este split em 2026-11-09).

## Contexto

Ledger de pagamentos estilo Pix simplificado: criar conta, depositar, transferir, extrato.
Regras duras: nenhuma transferência processada 2x (idempotência), saldo nunca negativo,
extrato auditável.
Base SQL (`V1`): `account(check balance>=0, version)`, `transaction(idempotency_key UNIQUE)`,
`ledger_entry` append-only (BIGSERIAL). Insumos históricos fora do projeto:
`~/Documentos/ledger-api-insumos/`.
