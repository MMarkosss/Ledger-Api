# Slice `polish` — encerrado em 2026-11-18

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> Q1/Q2 fecham o item 1 do polimento; Q3 fecha o item 2.

## Concluídos

- [x] **Q1** Fechar a construção do `LedgerEntry` — removido o `@Builder` e o construtor
  público. A criação passa por `LedgerEntry.credit(...)`/`LedgerEntry.debit(...)`, com
  validação de IDs, `amount > 0` e `balanceAfter >= 0`. A entidade ganhou `@Immutable` e o
  repositório foi estreitado para `Repository<LedgerEntry, Long>` com `save` e leitura,
  sem `delete`/`deleteAll`. `DepositAttemptService` passou a usar a factory.
  `LedgerEntryTest` cobre direção, campos e entradas inválidas.
- [x] **Q2** Append-only no banco — `V3__ledger_entry_append_only.sql` instala trigger
  `BEFORE UPDATE OR DELETE`, com SQLSTATE `23514`. `LedgerEntryAppendOnlyIT` prova que
  `UPDATE` e `DELETE` via JDBC são rejeitados e que o lançamento permanece. A limpeza dos
  ITs passou de `DELETE` para `truncate table ledger_entry, transaction, account restart
  identity`, sem `CASCADE`. ADR-009 criado; ADR-008 atualizado.
- [x] **Q3** Unificar DTOs de conta — `AccPostRes` removido. `POST /accounts` e
  `GET /accounts/{id}` usam `AccDetailRes`, na ordem `id, holderName, document, balance`.
  `AccountITSupport`/`DepositITSupport` atualizados. A ordem das propriedades JSON não é
  contrato; os contract tests existentes continuam provando o comportamento.

## Verificação

`./mvnw clean verify` verde em 2026-11-18:

- 14 testes unitários/contexto (8 `AccountTest`, 2 `IdempotencyHashTest`, 3
  `LedgerEntryTest`, 1 contexto);
- 19 testes de integração:
  - contas: 8;
  - depósitos: 10;
  - ledger: 1 (`LedgerEntryAppendOnlyIT`);
- 0 e2e.

Distribuição: 14/19/0. O novo IT prova uma invariante dura (append-only no banco); o desvio
do alvo 70/20/10 segue registrado no `TODO.md`, sem ser declarado convergente.

## Decisões aplicadas

- **ADR-009** — `LedgerEntry` append-only na construção (factories + `@Immutable` +
  repositório enxuto) e na persistência (trigger no banco). A trigger é a barreira contra
  SQL acidental; o dono da tabela ainda pode desabilitá-la deliberadamente, decisão
  operacional aceita.
- **ADR-008 atualizado** — a limpeza centralizada deixou de usar `DELETE` na ordem FK e
  passou a usar `TRUNCATE` explícito sem `CASCADE`, consequência do ADR-009. A ordem dos
  ITs continua irrelevante; `TRUNCATE ... CASCADE` continua rejeitado.
- **ADR-001 a ADR-007** não foram alterados. O kernel do ledger continua sem depender de
  `accounts`; a ligação `Account.credit/debit` + lançamento permanece responsabilidade do
  service da fatia.

## Limite conhecido

O Java não prova que `Account.credit/debit` foi chamado antes da criação do lançamento. Para
isso seria necessário acoplar `ledger` a `accounts`, o que viola o ADR-007. A defesa adotada
é: factories explícitas, service como único transaction script, `@Immutable`, repositório
sem delete e trigger no banco. O `LedgerEntryTest` cobre o contrato das factories; os ITs de
depósito provam o caminho real de criação.

## Término

Slice `polish` encerrado em 2026-11-18. Transferência e extrato seguem fora deste slice.
