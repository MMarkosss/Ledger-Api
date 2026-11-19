# Slice `ledger-context` — encerrado em 2026-11-19 e revertido

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração original é preservada (LC1–LC4).
>
> **Reversão:** o ADR-011 substituiu o ADR-010 em 2026-11-19. `Account` voltou para
> `features/accounts`; `LedgerPostingService` e `AccountDeletionService` foram removidos;
> a opção A (dependência explícita) foi adotada. Ver
> [`slice-explicit-ledger-dependency.md`](slice-explicit-ledger-dependency.md).

## Contexto

A revisão das “independências” do projeto mostrou que o ADR-007 tinha separado duas coisas:
não duplicar entidades por fatia (correto) e proibir `features/ledger` de conhecer conta
(incorreto para este domínio). O código já violava a segunda por caminhos escondidos:
query nativa em `ledger_entry`, constante da FK, exceção de conta com nome de ledger e
`balanceAfter` combinado no service de depósito.

## Concluídos

- [x] **LC1** Decisão e mapa — ADR-010 criado; ADR-007 marcado `superseded`; ADR-009
      emendado; mapa de decisões atualizado. Sem código.
- [x] **LC2** Posting service e ownership — `Account`, `AccountRepository` e exceções de
      domínio movidos para `features/ledger`; `LedgerPostingService` criado; `credit/debit`
      e factories de `LedgerEntry` package-private; `DepositAttemptService` usa o posting
      service; `DepositHappyPathIT` prova `balance_after == account.balance`.
- [x] **LC3** Deleção no ledger — `AccountDeletionService` criado; query nativa de
      `AccountRepository` removida; `LedgerEntryRepository.existsByAccountId` (D6 pago);
      `AccountDeletionIT` cobre `204` sem lançamento e `409` com lançamento.
- [x] **LC4** Limpeza de fronteira — `InvalidIdempotencyKeyException` movido de
      `features/ledger` para `shared.web`; canonicalização por operação documentada;
      README, TODO, ADRs e história sincronizados.

## Rodada final

`./mvnw clean verify` verde em 2026-11-19:

- 14 testes unitários/contexto (8 `AccountTest`, 2 `IdempotencyHashTest`,
  3 `LedgerEntryTest`, 1 contexto);
- 21 testes de integração (8 contratos de conta, 10 de depósito, 2 de deleção,
  1 de append-only);
- 0 e2e.

Distribuição: 14/21/0. O desvio do alvo 70/20/10 continua registrado; os dois ITs novos
protegem regra de negócio (deleção bloqueada) e a prova de `balance_after`, não detalhe de
biblioteca.

## Decisões e documentos

- **ADR-010** — conta e ledger no mesmo bounded context; `Account` e `LedgerEntry`
  permanecem aggregates separados; `LedgerPostingService` é a única porta de escrita.
- **ADR-007** — `superseded` por ADR-010; a parte de não duplicar entidades continua válida.
- **ADR-009** — emendado: factories package-private usadas pelo posting service; a
  alternativa “mover criação para `Account`” foi substituída pela variante no mesmo contexto.
- **ADR-001** — `Account` mudou para `features/ledger`; `credit/debit` package-private.
- **ADR-003/005** — implementação da tradução de constraint/FK movida para
  `AccountDeletionService`; `ConstraintNames` no ledger; FK continua árbitro.
- **ADR-006** — hash/replay preservados; payload é específico da operação;
  `InvalidIdempotencyKeyException` saiu do domínio.
- **ADR-008** — sem alteração; a limpeza `TRUNCATE` continua centralizada.

## Limites conhecidos

- `Account.balance` e `ledger_entry.balance_after` continuam duplicando o mesmo fato: um é
  saldo materializado, o outro é snapshot auditável. Na época, a consistência era garantida
  pelo posting service; após o ADR-011, ela é responsabilidade do transaction script do
  use case. O modelo com ledger como fonte da verdade fica como experimento futuro.
- Nenhuma migração de banco foi necessária.
- Nenhuma mudança de contrato HTTP; o JSON de conta continua igual.
