# ADR-010 — Conta e ledger no mesmo contexto; posting service

**Status:** superseded by [ADR-011](011-dependencia-explicita-accounts-ledger.md) em 2026-11-19
**Data:** 2026-11-19
**Origem:** revisão das “independências” do projeto + slice `ledger-context`

> Preservado como história. A opção A (“só tornar a dependência explícita”) foi
> reescolhida pelo ADR-011: `Account` voltou para `features/accounts`, o posting service e
> o `AccountDeletionService` foram removidos, e `balanceAfter` voltou a ser uma convenção
> explícita do use case.

## Contexto

O ADR-007 misturou duas decisões diferentes:

1. não duplicar `Transaction` e `LedgerEntry` por fatia — continua correta;
2. proibir `features/ledger` de conhecer `accounts` — não se sustenta neste domínio.

O código já expressava a dependência de conta para ledger, só que escondida:

- `AccountRepository.existsLedgerEntryFor` consultava `ledger_entry` por SQL nativo;
- `ConstraintNames.LEDGER_ENTRY_ACCOUNT_FK` conhecia o nome da FK do ledger;
- `AccountHasLedgerEntriesException` era uma exceção de conta que só existia por causa
  do ledger;
- `DepositAttemptService` chamava `Account.credit` e depois `LedgerEntry.credit`, deixando
  `balanceAfter` como uma convenção do service;
- `LedgerEntry` já era modelado por `account_id`, `counterparty_id` e `balance_after`.

O ADR-009 chegou a afirmar que o Java não conseguiria provar que `Account.credit/debit` foi
chamado sem acoplar `ledger` a `accounts`. Essa premissa nasceu do ADR-007; com conta e
ledger no mesmo contexto, a prova volta a ser possível.

## Decisão

1. **Um bounded context, dois aggregates.** `features/ledger` é o contexto do ledger e
   possui `Account`, `LedgerEntry`, `Transaction`, repositórios, posting e política de
   deleção de conta. `Account` e `LedgerEntry` continuam aggregates/tabelas separados; não
   há coleção de lançamentos dentro de `Account`.
2. **Uma única porta de escrita.** `LedgerPostingService` é a única classe que muta saldo e
   cria lançamento. `Account.credit/debit` e as factories de `LedgerEntry` são
   package-private; a fatia de depósito chama o posting service.
3. **`balanceAfter` nasce da mesma instância.** O posting service lê o saldo da conta logo
   depois da mutação e grava o lançamento na mesma transação. A invariante deixa de ser uma
   convenção entre dois services.
4. **Deleção de conta é regra do ledger.** `AccountDeletionService` concentra o pré-check e
   a tradução da FK; `AccountService.deleteAcc` delega. A FK continua sendo o árbitro
   (ADR-005).
5. **Direção de dependência.** `accounts`, `deposits`, `transfers` e `statements` são
   adapters/use cases que dependem de `features/ledger`. `AccountService`, controller e DTOs
   de conta continuam em `features/accounts`; o domínio não depende de DTO web.
6. **Idempotência continua no ledger.** `Transaction`, `IdempotencyHash` e
   `IdempotencyConflictException` ficam no contexto. O payload (`accountId|amount` no
   depósito; `source|destination|amount` na futura transferência) é específico da operação.
   `InvalidIdempotencyKeyException` sai do domínio para `shared.web`, porque é validação de
   header HTTP.

```text
account/controller/DTO  ─┐
deposits                ─┼─> features/ledger
transfers (futura)      ─┤     Account + LedgerEntry + Transaction
statements (futura)     ─┘     LedgerPostingService + AccountDeletionService
```

## Execução

- **LC1** — ADR-010, mapa de decisões e status do ADR-007 como `superseded`.
- **LC2** — `Account`/repositório/exceções movidos para `features/ledger`;
  `LedgerPostingService` criado; `credit/debit` e factories encapsulados; depósito usa o
  posting service; `DepositHappyPathIT` compara `balance_after` com o saldo da conta.
- **LC3** — `existsLedgerEntryFor` substituído por
  `LedgerEntryRepository.existsByAccountId` (D6); `AccountDeletionService`; `AccountDeletionIT`
  cobre `204` e `409`.
- **LC4** — `InvalidIdempotencyKeyException` movido para `shared.web`; documentação e
  rodada integral.

Não houve mudança de schema, contrato HTTP ou estratégia de testes.

## Alternativas rejeitadas (steelman)

- **Só tornar a dependência explícita (A)** — mover a query e a exceção resolveria o
  hidden dependency, mas `balanceAfter` continuaria pairado por convenção no service.
- **Manter o ADR-007 (D)** — preserva uma direção de dependência que o próprio código já
  viola por SQL, FK e exceção.
- **Um único aggregate JPA com `@OneToMany` de lançamentos** — daria consistência imediata,
  mas carregaria histórico ilimitado e transformaria o ledger em parte do estado mutável da
  conta. Rejeitado; a consistência fica no posting service e na transação.
- **Ledger como fonte da verdade do saldo (C)** — eliminaria a duplicação
  `account.balance`/`ledger_entry.balance_after`, mas muda concorrência, leitura, replay e
  provavelmente o schema. Fica como experimento futuro, com gatilho explícito; não é
  pré-requisito para transferência.

## Consequências

- `features/ledger` passa a depender conceitualmente de conta porque é o mesmo contexto; a
  dependência fica explícita no código, não escondida em SQL.
- `features/accounts` continua existindo como adapter/application; ele depende do ledger,
  nunca o contrário.
- `Account` e `LedgerEntry` continuam entidades separadas. `LedgerEntry` segue append-only
  (ADR-009).
- O depósito não consegue mais mutar saldo ou criar lançamento diretamente: a API
  package-private torna o bypass um erro de compilação.
- `transfers` nasce usando `LedgerPostingService`, sem repetir o transaction script de
  depósito.
- ADR-007 fica `superseded`; ADR-009 é emendado; ADR-001/003/005 têm a implementação
  atualizada; ADR-006 preserva hash/replay e explicita o payload por operação.
- A suíte passa a 35 testes: 14 unit/contexto, 21 integração, 0 e2e. O desvio do alvo
  70/20/10 continua registrado no `TODO.md`.

## Gatilho de reabertura

- Conta e ledger precisarem ser deployados/evoluídos como serviços separados.
- O ledger precisar ser genérico, sem conta como conceito central.
- A duplicação `account.balance`/`ledger_entry.balance_after` passar a causar divergência
  real ou custo de leitura; nesse caso avaliar o modelo com ledger como fonte da verdade.
