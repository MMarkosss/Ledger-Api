# Slice `explicit-ledger-dependency` — encerrado em 2026-11-19

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração ED1–ED3 é preservada.

## Contexto

O ADR-010 tinha movido `Account` para `features/ledger` e criado o `LedgerPostingService`
como única porta de escrita. A decisão resolvia o hidden dependency, mas deixava o PBF
inconsistente e não transformava `balanceAfter` em invariante. A opção A, antes rejeitada
no ADR-010, foi reescolhida: manter a conta na fatia de conta e tornar a dependência
`accounts -> ledger` explícita.

O commit `4289f62` empacotou Q1–Q3 + LC1–LC4. Por isso a reversão foi seletiva: Q1/Q2
(append-only), Q3 (DTO único), `TRUNCATE` e `InvalidIdempotencyKeyException` em
`shared.web` foram mantidos; apenas a ownership de conta, o posting service e o deletion
service foram revertidos.

## Concluídos

- [x] **ED1** `Account`, `AccountRepository` e exceções de domínio de conta voltaram para
      `features/accounts`; `LedgerPostingService` e `AccountDeletionService` removidos;
      `Account.open` mantido; Q1/Q2 append-only e Q3 DTO único preservados.
- [x] **ED2** `AccountService` passou a depender explicitamente de
      `LedgerEntryRepository.existsByAccountId`, `LedgerConstraintNames` e
      `AccountHasLedgerEntriesException`; `DepositAttemptService` orquestra `Account.credit`
      + `LedgerEntry.credit`; `AccountTest` voltou para `features/accounts`;
      `AccountDeletionIT` mantido.
- [x] **ED3** ADR-011 criado; ADR-010 marcado `superseded`; ADR-007/009/001/003/005/006
      sincronizados; README/TODO/histórias atualizados.

## Rodada final

`./mvnw clean verify` verde em 2026-11-19:

- 14 testes unitários/contexto (8 `AccountTest`, 2 `IdempotencyHashTest`,
  3 `LedgerEntryTest`, 1 contexto);
- 21 testes de integração (8 contratos de conta, 10 de depósito, 2 de deleção,
  1 de append-only);
- 0 e2e.

Distribuição: 14/21/0. O desvio do alvo 70/20/10 continua registrado no `TODO.md`.

## Decisões e documentos

- **ADR-011** — conta fora do ledger; dependência `accounts -> ledger` explícita;
  `balanceAfter` é snapshot do use case, não invariante; sem posting service.
- **ADR-010** — `superseded` por ADR-011; preservado como história.
- **ADR-007** — volta a `accepted (amended by 011)`; kernel separado, com a dependência
  explícita de `accounts` para deleção.
- **ADR-009** — append-only mantido; factories públicas validadas; `balanceAfter` não
  invariante.
- **ADR-001/003/005** — implementação de conta/constraint/deleção voltou para
  `features/accounts`, com o nome da FK e a exceção no ledger.
- **ADR-006** — hash/replay preservados; `InvalidIdempotencyKeyException` na borda.
- **ADR-008** — sem alteração; `TRUNCATE` centralizado continua.

## Limites conhecidos

- `Account.balance` e `ledger_entry.balance_after` continuam duplicando o mesmo fato: um é
  saldo materializado, o outro é snapshot auditável. A consistência no depósito é garantida
  pelo transaction script e pelo teste; não é uma invariante cross-aggregate.
- Nenhuma migração de banco foi necessária.
- Nenhuma mudança de contrato HTTP.
