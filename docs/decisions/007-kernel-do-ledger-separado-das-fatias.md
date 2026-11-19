# ADR-007 — Kernel do ledger separado das fatias

**Status:** accepted (amended by [ADR-011](011-dependencia-explicita-accounts-ledger.md) em 2026-11-19)
**Data:** 2026-11-14 (debate); fechamento 2026-11-17
**Origem:** item 3 do backlog + L1–L3

> O ADR-010 tentou superseder esta decisão, mas foi revertido pelo ADR-011. O kernel
> continua separado e o ledger não conhece conta; o que muda é que `accounts` pode
> depender do ledger explicitamente para a política de deleção.

## Contexto

`features/ledger` misturava o kernel compartilhado do ledger com a fatia de depósito.
Transferência e extrato ainda não existiam, mas já estava claro que `Transaction`,
`LedgerEntry`, os enums e os repositórios seriam usados por mais de uma fatia. A
duplicação dessas entidades por fatia quebraria a consistência do ledger; manter tudo em
`features/ledger` esconderia a fronteira entre núcleo e operação.

## Decisão

`features/ledger` é o **kernel compartilhado**: entidades, enums, repositórios,
exceções de idempotência e `IdempotencyHash`. Não contém controller, service de operação,
DTOs de fatia nem resultado de operação.

As operações são fatias verticais de primeiro nível:

```text
features/
  accounts/
  deposits/
  transfers/    # futura
  statements/   # futura
  ledger/       # kernel
```

Direção de dependência:

```text
accounts   -> ledger          # deleção: existsByAccountId + FK + exceção do ledger
deposits   -> accounts + ledger
transfers  -> accounts + ledger
statements -> ledger (+ accounts se a resposta precisar)
ledger     -> nenhuma fatia, nem accounts
```

Entidades do ledger não são duplicadas por fatia. `IdempotencyHash` concentra a
canonicalização do valor e o SHA-256 usados pela idempotência.

## Execução

- **L1** extraiu `IdempotencyHash` para o kernel; `DepositAttemptService` passou a usá-lo.
- **L2** moveu controller, services, resultado, DTOs e testes de depósito para
  `features/deposits`; a mudança de pacote dos testes foi mecânica, sem fatiar ou renomear
  classes.
- **L3** fechou com `./mvnw clean verify` verde, ADR-007, história do slice e mapa de
  decisões sincronizado.

Não houve mudança de comportamento, contrato HTTP ou schema. ADR-004 e ADR-006 continuam
válidos.

## Alternativas rejeitadas (steelman)

- **Subpacotes sob `features/ledger`** (`ledger/deposits`, `ledger/transfers`) — menos
  churn, mas mantém `ledger` como guarda-chuva e enfraquece a separação vertical.
- **Kernel em `shared/ledger`** — explicita o compartilhamento, mas trata domínio do ledger
  como infraestrutura compartilhada e aumenta o churn sem ganho de fronteira.
- **Duplicar entidades por fatia** — daria autonomia artificial às fatias, mas criaria
  modelos divergentes para o mesmo ledger; rejeitado.

## Consequências

- `features/ledger` não depende de `accounts` nem de fatias.
- `transfers` e `statements` nascem em pacotes próprios e reutilizam o kernel.
- Imports e testes de depósito passam a apontar para `features.deposits`.
- No fechamento L3, o fatiamento de testes (item 4) e o `AccountContractIT` (item 5) não
  foram tocados; ambos foram encerrados depois, no slice `test-isolation` (ADR-008), sem
  alterar esta decisão de kernel.
- O kernel deve permanecer pequeno; lógica específica de operação não entra nele.

## Gatilho de reabertura

O kernel crescer com regra específica de uma operação, ou uma fatia precisar de modelo de
persistência divergente do ledger compartilhado.
