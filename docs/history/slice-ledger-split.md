# Slice `ledger-split` — encerrado em 2026-11-17

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração original é preservada (2(c): nada é apagado/renumerado).

## Concluídos

- [x] **L1** Extrair `IdempotencyHash` para o kernel — canonicalização do valor e SHA-256
      saíram de `DepositAttemptService`; o kernel passou a ser reutilizável por futuras
      operações. Unitário de normalização + ITs de depósito verdes.
- [x] **L2** Mover depósito para `features/deposits` — controller, services, resultado,
      DTOs e testes acompanharam o pacote. A mudança dos testes foi apenas de pacote; não
      houve fatiamento nem renomeação de classes.
- [x] **L3** Fechar separação — `./mvnw clean verify` verde, ADR-007, esta história e mapa
      de decisões sincronizado.

## Rodada final

`./mvnw clean verify` verde em 2026-11-17:

- 11 testes unitários/contexto (8 `AccountTest`, 2 `IdempotencyHashTest`, 1 contexto);
- 18 testes de integração (8 `AccountContractIT`, 8 `DepositContractIT`,
  2 `DepositConcurrencyIT`);
- 0 e2e — a jornada continua nascendo com transferência + extrato.

Distribuição: 11/18/0. O desvio do alvo 70/20/10 segue registrado no `TODO.md`; não foi
declarado convergente.

## Debate de arquitetura (fechamento)

Pergunta adversarial: *o kernel compartilhado é necessário, ou cada fatia poderia ter seu
próprio modelo de ledger?*

| Opção | Ideia | Steelman |
|---|---|---|
| **A (mantida)** | `features/ledger` kernel + `features/deposits`/`transfers`/`statements` como fatias irmãs | Separa o núcleo do ledger das operações; evita duplicar entidades e mantém a direção de dependência explícita. |
| **B** | Subpacotes sob `features/ledger` (`ledger/deposits`, ...) | Menos churn e comunica que tudo é ledger; legítimo se `ledger` for tratado como bounded context. |
| **C** | Kernel em `shared/ledger` | Explicita o compartilhamento, mas trata domínio como infraestrutura e aumenta o churn. |

A opção A foi mantida porque `accounts` já é uma fatia de primeiro nível; depósito,
transferência e extrato são fatias irmãs, não subpastas de uma entidade. Duplicar
`Transaction`/`LedgerEntry` por fatia foi rejeitado: criaria modelos divergentes para o
mesmo ledger. O custo de A ficou limitado a imports e movimentação de pacote, coberto pela
suíte integral.

## Decisões aplicadas

- **ADR-007** — kernel do ledger separado das fatias; `features/ledger` sem controller ou
  service de operação; entidades não duplicadas; dependência `fatia -> kernel`.
- **ADR-004** e **ADR-006** continuam válidos; a implementação de idempotência apenas
  passou a viver em `features/deposits` e o hash no kernel.
- **ADR-001**, **ADR-002**, **ADR-003** e **ADR-005** não foram alterados.

## Achados sem decisão nova

- `features/ledger` deixou de importar `features/accounts`; a dependência de conta ficou
  restrita às fatias que operam conta.
- O kernel não tem controller, service de operação nem DTO de fatia.
- Não houve mudança de contrato HTTP, comportamento ou schema.
- O fatiamento de testes (item 4) e `AccountContractIT` (item 5) permanecem congelados.
- D18 continua discussion-only, com gatilho no item 5.

## Dívidas

Nenhuma dívida nova. D6, D8, D9 e D13 mantêm os gatilhos registrados no `TODO.md`.

## Revisão de decisões — 2026-11-17

- Revisão feita depois do debate de arquitetura e antes do fechamento do slice.
- ADR-001 a ADR-007 revisados: todos `accepted`, nenhum `superseded`.
- ADR-002 e ADR-004 aguardam apenas o gate externo R10; não há divergência de código.
- ADR-003 registra a leitura via `ConstraintViolationException` do Hibernate; D13 continua
  aberto para a arquitetura de erros curada.
- ADR-006 teve a implementação atualizada para `features.deposits` + `IdempotencyHash` no
  kernel.
- ADR-007 registra a separação kernel/fatias.
- `README.md` não mudou porque o comportamento externo não mudou; `TODO.md`, mapa e esta
  história foram sincronizados no mesmo fechamento.

## Término

Slice `ledger-split` encerrado em 2026-11-17. Transferência e extrato seguem fora deste
slice.

> Nota posterior (2026-11-17): o fatiamento de testes (item 4) e o `AccountContractIT`
> (item 5), ainda congelados no fechamento L3, foram encerrados no slice `test-isolation`
> (TI1–TI4, ADR-008), sem alterar a decisão de kernel.
