# ADR-001 — Invariantes na entidade `Account`

**Status:** accepted (emenda do [ADR-010](010-conta-e-ledger-no-mesmo-contexto.md) revertida pelo [ADR-011](011-dependencia-explicita-accounts-ledger.md) em 2026-11-19)
**Data:** 2026-11-08 (debate SRP); ratificado 2026-11-09
**Origem:** A14 (review externa) + D4 + debate S4/SRP

> Nota ADR-011: `Account` voltou para `features/accounts`; `credit`/`debit` são públicos
> porque o use case orquestra conta e ledger em pacotes diferentes. `rename` continua
> público. O pareamento com o lançamento é convenção do use case, não invariante do
> aggregate.

## Contexto

O saldo era dono de um operator bean (`AccountBalanceOperator`) e o nome era mutado por um
`AccountUpdater`; `applyDelta` nu aceitava qualquer delta. A validação vivia fora do agregado
e podia ser esquecida por qualquer chamador; `IllegalArgumentException` virava 500 no
fallback. O P-A tinha vetado `rename` na entidade por SRP.

## Decisão

`Account` expõe `credit(amount)`, `debit(amount)` e `rename(name)` com checagens e exceções
de domínio (`InvalidAmountException`, `InsufficientBalanceException`,
`InvalidHolderNameException`). Divisão: DTO = formato; entidade = invariante para qualquer
chamador; CHECK do banco = piso. `rename` normaliza (trim) e aplica 3–80.
`AccountBalanceOperator`, `AccountUpdater`, `applyDelta` e os setters de `holderName`/`balance`
morrem; os 8 unitários migram para `AccountTest`.

## Alternativas rejeitadas (steelman)

- **Operator bean dono do saldo** — validação fora do agregado; qualquer chamador pode
  esquecer; IAE → 500.
- **Split domínio/persistência (S4)** — cerimônia sem ganho neste tamanho.
- **Entidade espelho do schema, sem regra** — o critério "uma classe faz uma coisa" foi
  trocado por "quem pede a mudança": saldo e nome mudam por decisão do agregado.

## Consequências

- Reversão consciente do veto de P-A (`rename` na entidade) — registrada no history.
- D4 fecha quando R5 estiver verde (8 testes migrados e passando).
- D11 (IAE no advice) fica superseded: o caminho deixa de existir; reabre se R5/ADR-001
  for revertido.
- Defesa em profundidade: DTO (formato) → entidade (invariante) → CHECK (piso).

## Gatilho de reabertura

Se a entidade ganhar relações e os invariantes virarem orquestração multi-agregado.
