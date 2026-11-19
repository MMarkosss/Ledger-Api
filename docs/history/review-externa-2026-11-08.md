# Review externa do slice Accounts — 2026-11-08

> Fonte: `TODOv1.md` (insumo externo; 162 linhas / 23.459 bytes; SHA-256
> `8b2bff76ca77df2584e5353e1358fccb65e19874236676ef4cb575e45d002f15`).
> Triagem íntegra: achados **A10–A21** e ações **R1–R10**. As ações morrem ao executar;
> o estado vivo está no `TODO.md`. A1–A9 (debate interno) estão em `slice-accounts.md`.

## Achados A10–A21

| # | Achado | Estado |
|---|---|---|
| A10 | Advice mapeia `DataIntegrityViolationException` por tipo com mensagem fixa — mente no DELETE (FK vira "Documento já cadastrado") e mentiria no CHECK ("Documento" para saldo) | pago por R1 (tradução no service) |
| A11 | `handleFallback` engole exceção sem log — todo 500 é buraco negro | interino pago por R2; arquitetura curada = D13 |
| A12 | `postgres:latest` no Testcontainers × V1 exige `uuidv7()` (PG18+) — migration explode fora do PG18 | pago por R3 |
| A13 | properties sem `ddl-auto=validate` / `open-in-view=false` / exposição actuator — Prometheus é código morto | pago por R4 |
| A14 | Invariantes fora do agregado: operator é dono do saldo (lança `IllegalArgumentException` → 500 via fallback), `applyDelta` nu aceita `-999999`, updater é builder-mutador com regra mais frouxa que o DTO (não-branco vs 3–80) | pago por R5 (debate SRP: entidade dona; defesa em profundidade na ordem certa) |
| A15 | Índice duplicado `idx_entry_tx`/`idx_entry_idem` (ambos `transaction_id`) — write amplification na tabela mais quente; erro do sketch do tutor | pago por R6 |
| A16 | TOCTOU do DELETE confirmado (janela entre `exists` e `delete`) — FK é a rede; dado nunca corrompe, mensagem era que mentia | mensagem: R1; documentação "FK como árbitro" → D16 |
| A17 | Pacote `MMarkoss` com maiúscula (viola convenção Java); `/contas` PT × codebase EN; `AccGetRes`/`AccGetOneRes` indistinguíveis; `findOneByIdAcc` | pago por R7 |
| A18 | PUT ≡ PATCH com mesmo body obrigatório — PATCH sem semântica parcial não é PATCH | D15 (revisit) |
| A19 | `compose.yaml` com placeholder do Initializr (`mydatabase/secret`); README 0 bytes; sem CI | pago por R8/R9 |
| A20 | Post do LinkedIn ("UUIDv7 habilita batching") × P10 (BIGSERIAL/IDENTITY desabilita batching no `ledger_entry`) — um dos dois mente | gate R10 (alinhar antes de publicar) |
| A21 | Dono do ID não documentado: `@UuidGenerator(VERSION_7)` (app) × `DEFAULT uuidv7()` (banco) — decisão existe, registro não; DEFAULT é código morto para writes da app (INSERT sempre leva id), piso para writers não-app | D14 |

## Ações R1–R10 (triagem; estado vivo no TODO.md)

> **Ordem de execução (voz da época, 2026-11-08):** "R1–R8 bloqueiam o P11 (o P10 pode seguir
> em paralelo — não toca em `account`). R9–R10 bloqueiam a publicação do post do LinkedIn,
> não o código." **Nota 2026-11-09:** o rótulo "bloqueiam" foi substituído por "valor
> econômico, não bloqueio" (arbitragem 2(a)); a ordem P10 paralelo / P11 com R1–R8 pagos /
> R9–R10 gates permanece.

- **R1** Tradução de constraint no service (fim do dispatch por tipo no advice) — o advice
  perde o `handleConflict` inteiro: `DataIntegrityViolationException` não traduzida =
  incidente → fallback + log. `AccountService.createAcc`: pre-check `existsByDocument`
  (409 amigável) + `catch (DataIntegrityViolationException)` com **`saveAndFlush` dentro do
  try** → `DuplicateDocumentException` (constraint `account_document_key`, via
  `ConstraintNames` da feature). `deleteAcc`: mesmo padrão com a FK
  `ledger_entry_account_id_fkey` → `AccountHasLedgerEntriesException`. Gotcha: `save()`
  adia o INSERT para o flush — sem `saveAndFlush` o catch nunca vê a violação. Detecção por
  SQLState (`23505` × `23503`) + nome da constraint via `PSQLException`; após o catch a
  transação está condenada — traduzir e lançar.
- **R2** Fallback do advice com log — `log.error("{} {}", method, uri, ex)`: stack completa
  no log do servidor; resposta ao cliente permanece mensagem estática. Interino do D13.
- **R3** Pin `postgres:18` no `TestcontainersConfiguration` (hoje `postgres:latest`; a V1
  usa `uuidv7()`, que só existe no PG18+ — fora dele a migration explode com
  `function uuidv7() does not exist`). **Executado em 2026-11-09.**
- **R4** Três properties — `spring.jpa.hibernate.ddl-auto=validate` (entity divergente do
  schema = app não sobe), `spring.jpa.open-in-view=false`,
  `management.endpoints.web.exposure.include=health,info,prometheus` (sem esta, o
  `micrometer-registry-prometheus` do pom é código morto). **Executado em 2026-11-09.**
  Nota: o `validate` passou em 2026-11-08 com só `Account` mapeado; com o P10,
  `direction CHAR(1)` e colunas `TEXT` podem exigir `columnDefinition`/`JdbcTypeCode`.
- **R5** Invariantes para dentro da `Account` (decisão do debate SRP — fecha D4, supersede
  D11) — `credit(amount)`/`debit(amount)`/`rename(name)` com exceções de domínio
  (`InvalidAmountException`, `InsufficientBalanceException`, `InvalidHolderNameException`);
  `rename` normaliza (trim) e aplica a mesma regra do DTO (3–80); `applyDelta`,
  `AccountBalanceOperator` e `AccountUpdater` removidos; `@Setter(PACKAGE)` de `holderName`
  removido; testes: os 8 unitários migram para `AccountTest` com os mesmos casos.
- **R6** `V2__drop_redundant_ledger_index.sql` — `DROP INDEX idx_entry_idem;` (duplicado do
  `idx_entry_tx`, ambos sobre `transaction_id` — erro do sketch do tutor, admitido). Nunca
  editar a V1. **Executado em 2026-11-09.**
- **R7** Renomes em uma tacada — R7.1 pacote `MMarkoss` → `mmarkoss` (`git mv`); R7.2
  idioma da API: **EN** (veredito de 2026-11-09; rota `POST /accounts/{id}/deposits`);
  R7.3 `AccGetRes` → `AccSummaryRes`, `AccGetOneRes` → `AccDetailRes`;
  `findOneByIdAcc` → `findByIdOrThrow`.
- **R8** `compose.yaml` — `mydatabase/myuser/secret` → `ledger/ledger/ledger` (placeholder
  do Initializr não mora em repo de pagamentos). **Executado em 2026-11-09.**
- **R9** README + CI — workflow GitHub Actions (build + `mvn verify` com Testcontainers) +
  badge; README com as decisões de D14/D16. **Gate de publicação.**
- **R10** Gate de publicação do LinkedIn — alinhar o texto do post com o código: a claim
  "UUIDv7 habilita batching" contradiz o P10 (BIGSERIAL/IDENTITY desabilita batching no
  `ledger_entry` por design). Publicar só com README + CI verde + post alinhado.
  **Gate de publicação.**

## Triagem para ADRs

| Origem | ADR |
|---|---|
| A14 / debate SRP / D4 | **ADR-001** — invariantes na entidade |
| A21 / D14 | **ADR-002** — dono do ID |
| A10 / A5 / R1 | **ADR-003** — tradução de constraint no service |
| A20 / R10 | **ADR-004** — BIGSERIAL no `ledger_entry` |
| A16 / A7 | **ADR-005** — FK como árbitro do DELETE |
| P9 / P13 | **ADR-006** — idempotência hash + replay |

## Débitos que nasceram desta review

- **D13** — arquitetura de erros curada (de A11/R2).
- **D14** — ADR/README do dono do ID (de A21).
- **D15** — PATCH sem semântica parcial (de A18).
- **D16** — README das decisões (de A7/A16/A20/D14).
- **D17** — cirurgia de gravidade do TODO (split) — **pago em 2026-11-09** por este
  `PROCESS.md`/ADRs/history/TODO.

## Nota de proveniência

A reconstrução `TODO-v1-post-review.md` foi conferida contra este `TODOv1.md` em
2026-11-09: `diff -u` de **36 linhas / 1 hunk**; únicas diferenças, 1 linha em branco
(+1 byte) e 10 checkboxes removidos (−40 bytes); saldo **−39 bytes**. Conteúdo idêntico.
A errata do "concordo com todas" não contaminou o material: o v1 sempre foi a fonte
correta do PLANO.
