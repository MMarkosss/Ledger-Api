# Slice `transfers` — encerrado em 2026-11-22

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração TR0–TR5 é preservada.

## Contexto

A transferência foi a primeira operação a mutar **duas contas** na mesma transação. O
`V1` já tinha `transaction.type = TRANSFER`, `ledger_entry.counterparty_id` e dois
lançamentos por transação, então o slice não precisou de migração nova.

A decisão A do ADR-012 venceu a discussão de concorrência: carregar origem e destino com
`PESSIMISTIC_WRITE` em ordem determinística de UUID, sem retry interno. B1 (otimista sem
retry) e B2 (otimista com retry) foram rejeitadas porque não eliminam o deadlock de duas
linhas por construção.

## Concluídos

- [x] **TR0** Decisão e contrato — ADR-012 com entradas adversariais A/B1/B2;
      `POST /transfers`; resposta com os dois saldos; mesma conta em `422`; conflito de
      lock retryable em `409` distinto da idempotência. — pago 2026-11-21.
- [x] **TR1** Caminho feliz — controller/service/DTOs em `features/transfers`; débito na
      origem + crédito no destino + dois `ledger_entry` com `counterparty_id`; lock
      ordenado. Teste: `TransferHappyPathIT`. — pago 2026-11-21.
- [x] **TR2** Erros — mesma conta, origem/destino inexistente, saldo insuficiente, header
      e amount inválidos; rollback sem transação/lançamento. Teste:
      `TransferValidationIT`. — pago 2026-11-22.
- [x] **TR3** Idempotência — `201` novo, `200` replay com os `balance_after` armazenados,
      `409` para payload/tipo diferente, concorrência com a mesma chave. Teste:
      `TransferIdempotencyIT`. — pago 2026-11-22.
- [x] **TR4** Concorrência — chaves diferentes na mesma origem e direções opostas sem
      deadlock/`500`; lock timeout mapeado para `409` retryable. Teste:
      `TransferConcurrencyIT`. — pago 2026-11-22.
- [x] **TR5** Fechamento — D19 pago com `TransferPair` (value object package-private em
      `features.transfers`) e `TransferPairingTest`; README/TODO/história e mapa de
      decisões sincronizados; e2e/BDD mantidos no extrato (D8/D20). — pago 2026-11-22.

## Rodada final

`./mvnw clean verify` verde em 2026-11-22, com 53 testes:

- **15 unit/contexto** — `AccountTest` (8), `LedgerEntryTest` (3),
  `IdempotencyHashTest` (2), `TransferPairingTest` (1) e `LedgerApiApplicationTests` (1).
- **38 integração** — 10 de conta, 10 de depósito, 1 de append-only e 17 de transferência.
- **0 e2e** — a primeira jornada e2e/BDD nasce no extrato.

A distribuição 15/38/0 é o desvio conhecido em relação ao alvo 70/20/10; ela foi medida
sem declarar convergência. O e2e da transferência não foi criado porque duplicaria o
`TransferHappyPathIT` e, sem extrato, não provaria a leitura auditável nem a ordem do
ledger.

## Decisões e documentos

- ADR-012 permanece `accepted`; TR0–TR5 concluídos, D19 pago, D8/D20 abertos com gatilho
  no extrato.
- O `TransferPair` encapsula a mutação das duas contas e a criação das duas pernas com o
  snapshot posterior. É local à transferência e não recria o posting service central
  rejeitado pelo ADR-011.
- ADR-001, ADR-006 e ADR-011 foram atualizados no mapa de decisões para registrar o
  pareamento unitário, o reuso da idempotência e a orquestração explícita da transferência.
- `README.md` documenta o endpoint, os status `201/200/400/404/409/422`, o hash
  `source|destination|amount`, o lock ordenado e o timeout configurável.
- `TODO.md` marca o slice como encerrado, paga D19 e registra a suíte 15/38/0.

## Limites conhecidos

- Sem endpoint de extrato; a auditabilidade do cliente e a ordem dos lançamentos ainda não
  formam uma jornada e2e.
- `D8` (BDD) e `D20` (e2e da transferência) ficam para o extrato.
- `D13` (arquitetura de erros curada) continua aberto.
- O lock timeout padrão é `2s`, configurável por `LEDGER_TRANSFER_LOCK_TIMEOUT`; o `409`
  de concorrência é retryable e distinto do `IDEMPOTENCY_CONFLICT`.
