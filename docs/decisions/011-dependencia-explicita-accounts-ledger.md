# ADR-011 — Conta fora do ledger e dependência explícita

**Status:** accepted
**Data:** 2026-11-19
**Origem:** reversão do ADR-010 + opção A da revisão de contexto

> Substitui o [ADR-010](010-conta-e-ledger-no-mesmo-contexto.md). O
> [ADR-007](007-kernel-do-ledger-separado-das-fatias.md) volta a valer como kernel
> separado, emendado por esta decisão. O [ADR-009](009-ledger-entry-append-only.md)
> mantém o append-only, mas com factories públicas.

## Contexto

O ADR-010 colocou `Account` dentro de `features/ledger` e criou o `LedgerPostingService`
como única porta de mutação. A decisão resolvia o hidden dependency, mas deixava o PBF
inconsistente: `features/accounts` continuava existindo como adapter, enquanto o domínio
de conta morava em `features/ledger`. Para quem lê o projeto, `ledger` passava a ser um
guarda-chuva de conta + lançamento + transação.

As alternativas de package aggregates dentro de `ledger` também não fechavam:

- `package-private` não atravessa subpacotes; seria preciso tornar as mutações públicas
  e adicionar ArchUnit, ou criar gateways públicos que apenas movem o bypass de lugar;
- o `balanceAfter` continuaria sendo combinado pelo use case, não pelo aggregate;
- o custo de cerimônia cresceria sem resolver o problema central.

O que era real no diagnóstico do ADR-010 continua real: o código escondia a dependência
de conta no ledger por três caminhos:

- `AccountRepository.existsLedgerEntryFor` consultava `ledger_entry` por SQL nativo;
- `ConstraintNames.LEDGER_ENTRY_ACCOUNT_FK` conhecia o nome da FK do ledger;
- `AccountHasLedgerEntriesException` era uma exceção de conta que só existia por causa do
  ledger.

## Decisão

1. **Conta fica na fatia de conta.** `Account`, `AccountRepository` e as exceções de
   domínio de conta voltam para `features/accounts`. `LedgerEntry` e `Transaction`
   continuam no kernel `features/ledger`. Não há `Account` dentro do ledger.
2. **A dependência é explícita e unidirecional.** `features/accounts` depende de
   `features/ledger` para a política de deleção: `AccountService` usa
   `LedgerEntryRepository.existsByAccountId`, `LedgerConstraintNames` e
   `AccountHasLedgerEntriesException`. `features/ledger` não depende de `accounts`.
3. **Sem posting service e sem deletion service.** `LedgerPostingService` e
   `AccountDeletionService` deixam de existir. O `DepositAttemptService` é o transaction
   script que orquestra `Account.credit` e a criação do `LedgerEntry` na mesma transação.
4. **`balanceAfter` não é invariante.** `Account.balance` tem invariantes no aggregate;
   `ledger_entry.balance_after` é um snapshot histórico fornecido pelo use case. Não há
   garantia de modelo de que ele reflita o saldo da conta; o depósito escreve o snapshot
   logo após o `credit`, e o `DepositHappyPathIT` prova esse caminho, não uma invariante
   cross-aggregate.
5. **Append-only permanece no ORM e no banco.** `LedgerEntry` continua sem builder, com
   factories `credit`/`debit` validadas, `@Immutable`, repositório sem `delete` e trigger
   `V3` contra `UPDATE`/`DELETE`. As factories são públicas porque o caller agora é uma
   fatia; a persistência append-only é a barreira dura, não a visibilidade da factory.
6. **Idempotência na borda.** `InvalidIdempotencyKeyException` continua em `shared.web`;
   `IdempotencyHash`, `Transaction` e replay continuam no ledger, com payload específico
   da operação.
7. **Package aggregates dentro do ledger e conta como aggregate do ledger estão
   rejeitados.** O ledger é kernel; a conta pertence à fatia de conta. A opção de mover
   `Account` para o ledger fica como história no ADR-010.

```text
accounts  -> ledger          # deleção: existsByAccountId + FK + exceção do ledger
deposits  -> accounts + ledger
ledger    -> nenhuma fatia   # não importa accounts
```

## Alternativas rejeitadas (steelman)

- **Um contexto, dois aggregates + posting service (ADR-010)** — daria uma única porta de
  escrita, mas colocaria `Account` no ledger e deixaria o PBF inconsistente. Revertido.
- **Package aggregates dentro de `ledger` + ArchUnit** — melhoraria a árvore visual, mas
  exigiria mutações públicas e um teste de arquitetura; não resolveria o `balanceAfter` e
  adicionaria dependência/cerimônia para um projeto deste tamanho.
- **Gateways públicos por aggregate** — movem o bypass para `AccountMutationService` /
  `LedgerEntryService`; sem ArchUnit, não provam o pareamento.
- **Um único aggregate JPA com `@OneToMany`** — carregaria histórico ilimitado e trataria
  o ledger como estado mutável da conta. Continua rejeitado.
- **Manter a query nativa em `AccountRepository`** — esconderia de novo a dependência que
  esta decisão torna explícita.

## Consequências

- `Account.credit/debit` são públicos; o pareamento com o lançamento é uma convenção do
  use case, não uma garantia de compilação.
- `AccountService` passa a depender do ledger no construtor; a dependência aparece no
  código, não em SQL nativo.
- `AccountHasLedgerEntriesException` e `LedgerConstraintNames` vivem no ledger porque o
  ledger é dono dos lançamentos e da FK.
- `AccountDeletionIT` continua cobrindo `204` sem lançamento e `409` com lançamento.
- `InvalidIdempotencyKeyException` permanece na borda HTTP; a borda valida o header, o
  domínio cuida de hash/replay.
- A suíte permanece em 35 testes: 14 unit/contexto, 21 integração, 0 e2e.
- Transferência e extrato continuam fora deste corte. Quando a transferência chegar, ela
  deve repetir a orquestração de forma explícita; se a duplicação virar custo real, uma
  nova decisão pode introduzir um application service na fatia, sem mover `Account` para
  o ledger.

## Gatilho de reabertura

- A orquestração de conta + lançamento se repetir em depósito, transferência e extrato de
  forma custosa; nesse caso avaliar um application service na fatia, não um aggregate
  `Account` no ledger.
- `balance_after` divergir de `account.balance` em um caminho real; nesse caso o modelo de
  snapshot precisa ser revisto.
- O ledger precisar ser genérico ou evoluído como serviço separado, sem conta como
  conceito central.
