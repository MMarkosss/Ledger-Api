# Mapa de decisões

> Documento vivo. A fonte de cada decisão é o ADR correspondente; este arquivo só mapeia
> status e implementação para a revisão de fechamento de slice (regra 13 do `PROCESS.md`).
> A revisão completa acontece depois do debate de arquitetura e antes de fechar o slice.

**Última sincronização:** 2026-11-27

| ADR | Título | Status | Implementação | Observação |
|---|---|---|---|---|
| [001](001-invariantes-na-entidade.md) | Invariantes na entidade `Account` | accepted (emenda 010 revertida por 011) | `features.accounts.Account` + `AccountTest`; `credit/debit` públicos; `TransferPairingTest` cobre o pareamento | D4 fechado com R5; pareamento com lançamento é convenção do use case; D19 pago |
| [002](002-dono-do-id.md) | Dono do ID | accepted | `@UuidGenerator(VERSION_7)` + `DEFAULT uuidv7()` na V1; README | R10 é gate externo de publicação |
| [003](003-traducao-de-constraint-no-service.md) | Tradução de constraint no service | accepted | `AccountService` + `ConstraintNames`/`LedgerConstraintNames` + `ApiExceptionHandler` | D13 pago em 2026-11-27; fallback e contrato de erro curados no ADR-015 |
| [004](004-bigserial-no-ledger-entry.md) | BIGSERIAL no `ledger_entry` | accepted | `LedgerEntry` + `V1`; `V2` remove índice redundante; `StatementService` usa `(account_id, id)` | R10 é gate externo de publicação; o `entryId` global expõe volume (ADR-013) |
| [005](005-fk-como-arbitro-do-delete.md) | FK como árbitro do DELETE | accepted | `AccountService` + `LedgerEntryRepository.existsByAccountId` + FK da V1 | TOCTOU aceito; dependência accounts -> ledger explícita |
| [006](006-idempotencia-hash-e-replay.md) | Idempotência: hash + replay | accepted | `DepositAttemptService` + `TransferService` + `IdempotencyHash` + `insertIfAbsent` + ITs de depósito/transferência | P9/P11/P13 e TR3 fechados; `InvalidIdempotencyKeyException` em `shared.web` |
| [007](007-kernel-do-ledger-separado-das-fatias.md) | Kernel do ledger separado das fatias | accepted (amended by 011) | `features.ledger` como kernel; `accounts` pode depender dele para deleção; `statements` lê o kernel | O ADR-010 tentou superseder, mas foi revertido; `statements` nasceu como fatia irmã |
| [008](008-isolamento-e-fatiamento-de-testes.md) | Isolamento e fatiamento dos testes de integração | accepted | `IntegrationTestSupport` + supports; ITs por comportamento; limpeza `TRUNCATE`; e2e Cucumber usa o mesmo cleanup; sem `@Order` | TI1–TI4 fechados; limpeza atualizada pelo ADR-009; D8/D20 pagos no extrato |
| [009](009-ledger-entry-append-only.md) | `LedgerEntry` append-only: construção e persistência | accepted (amended by 011) | `LedgerEntry.credit/debit` públicas + `@Immutable` + repositório enxuto + `V3` trigger + `LedgerEntryAppendOnlyIT` | Q1/Q2 mantidos; `balanceAfter` é snapshot do use case |
| [010](010-conta-e-ledger-no-mesmo-contexto.md) | Conta e ledger no mesmo contexto; posting service | superseded by 011 | histórico | LC1–LC4 implementados e revertidos; opção A adotada |
| [011](011-dependencia-explicita-accounts-ledger.md) | Conta fora do ledger e dependência explícita | accepted | `features.accounts.Account` + `AccountService` -> `LedgerEntryRepository` + `DepositAttemptService`/`TransferService` orquestram | Substitui 010; D6 pago; transferência reusa a dependência explícita |
| [012](012-transferencia-duas-pernas-lock-deterministico.md) | Transferência: duas pernas e lock determinístico | accepted | `features.transfers` + `TransferPair` + lock `PESSIMISTIC_WRITE` ordenado por UUID + dois `ledger_entry` + timeout `409` retryable | TR0–TR5 fechados; D19 pago; D20 pago no extrato; D22 pago (timeout local); B1/B2 rejeitadas; sem migração |
| [013](013-extrato-leitura-por-conta-cursor-e-auditoria.md) | Extrato: leitura por conta, cursor e ordem auditável | accepted | `features.statements` + `Slice<LedgerEntry>` + `cursor`/`limit` + `desc` default e `asc` auditoria + `Statement*IT` + Cucumber e2e conciliando C/D | D8/D20 pagos; e2e cobre espelhamento, cadeia e saldo final; `entryId` global expõe volume; sem migração |
| [014](014-ciclo-de-vida-da-conta-hard-delete-e-soft-delete.md) | Ciclo de vida da conta: hard delete e soft delete adiado | accepted | `DELETE` guard + FK; sem `deleted_at` | D5 adiado; gatilho: desativar/ler conta com histórico |
| [015](015-arquitetura-de-erros-correlacao-e-conflito-otimista.md) | Arquitetura de erros, correlação e conflito otimista | accepted | `DomainException` + `ProblemDetail` + `TraceIdFilter` + `ApiExceptionHandler` + `ApiExceptionHandlerTest`/`ErrorContractIT` | D13 e D23 pagos; `X-Request-Id`/`traceId`; rota inexistente 404; conflito otimista 409 retryable; sem migração |

## Checklist de fechamento

Executar depois do debate de arquitetura e antes de marcar o slice como fechado:

1. Listar os ADRs tocados pelo slice.
2. Atualizar `Status`, implementação e observação nesta tabela.
3. Marcar ADR obsoleto como `superseded`; não apagar o arquivo.
4. Atualizar `README.md` se o comportamento externo mudou.
5. Atualizar `TODO.md` e registrar a revisão em `docs/history/slice-*.md`.
6. Se nenhum ADR foi tocado, registrar uma linha curta na história: “sem decisões novas
   ou alteradas”.

Nenhum ADR deve descrever comportamento que não existe; nenhuma decisão implementada deve
existir apenas no código. Se uma task mudar uma decisão antes do fim do slice, o ADR é
atualizado imediatamente; a revisão completa continua sendo no fechamento.
