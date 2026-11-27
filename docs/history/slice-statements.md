# Slice `statements` — encerrado em 2026-11-24

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração ST0–ST5 é preservada.

## Contexto

O extrato era a última operação do core e a peça que faltava para provar a jornada
completa. O `ledger_entry` já tinha `id` BIGSERIAL, `balance_after`, `counterparty_id` e o
índice `(account_id, id DESC)`; não houve migração.

A decisão de contrato foi registrar a leitura na conta
(`GET /accounts/{accountId}/statements`), paginar por cursor no `id` e oferecer
`order=desc` como default humano e `order=asc` como adição de auditoria. Expor o
`entryId` revela o volume global do BIGSERIAL; a consequência foi aceita e registrada no
ADR-013. O soft delete (D5) foi separado como decisão de ciclo de vida da conta no
ADR-014.

## Concluídos

- [x] **ST0** Decisão e contrato — ADR-013 para endpoint, cursor, `desc`/`asc`, payload e
      read model; ADR-014 para hard delete e D5 adiado. — pago 2026-11-24.
- [x] **ST1** Caminho feliz — `GET /accounts/{id}/statements` com depósito e transferência,
      ordem `desc`, `balanceAfter`, contraparte, `type` e `createdAt`; `asc` coberto.
      Teste: `StatementHappyPathIT`. — pago 2026-11-24.
- [x] **ST2** Paginação — cursor exclusivo, `limit`, `nextCursor`, `hasMore` e ausência de
      sobreposição em `desc` e `asc`. Teste: `StatementPaginationIT`. — pago 2026-11-24.
- [x] **ST3** Erros — conta inexistente `404`, conta sem lançamentos `200` vazio,
      `limit`/`cursor`/`order` inválidos `400`. Teste: `StatementValidationIT`. — pago
      2026-11-24.
- [x] **ST4** BDD/e2e — Cucumber 8 + Spring + Testcontainers; uma jornada cobre D8 e D20:
      criar contas, depositar, transferir, repetir a mesma chave e conciliar os dois
      extratos. A conciliação afirma o par C/D (mesmo `transactionId`/`amount`, direções
      opostas e contrapartes cruzadas), a continuidade da cadeia de `balance_after` e
      `saldo final == balance_after` mais recente. Teste: `LedgerJourneyE2E`. — pago
      2026-11-24.
- [x] **ST5** Fechamento — README, TODO, ADR-013/014, mapa de decisões e esta história;
      suíte integral verde. — pago 2026-11-24.

## Rodada final

`./mvnw clean verify` verde em 2026-11-24, com 61 testes:

- **15 unit/contexto** — `AccountTest` (8), `LedgerEntryTest` (3),
  `IdempotencyHashTest` (2), `TransferPairingTest` (1) e `LedgerApiApplicationTests` (1).
- **45 integração** — 10 de conta, 10 de depósito, 1 de append-only, 17 de transferência e
  7 de extrato.
- **1 e2e** — `LedgerJourneyE2E`, o cenário Cucumber da jornada completa.

Distribuição: **15/45/1**. O alvo 70/20/10 continua distante; a primeira jornada e2e agora
existe e o desvio segue registrado no `TODO.md`, sem declarar convergência.

## Reforço do e2e (2026-11-26)

- O cenário Cucumber passou a conciliar os dois extratos, não apenas a ler cada lado.
- Asserções adicionadas: mesmo `transactionId` e `amount`, direções opostas, contrapartes
  cruzadas, continuidade da cadeia de `balance_after` por conta e saldo final igual ao
  `balance_after` mais recente.
- A suíte permanece com 61 testes; o cenário continua sendo o único e2e.

## Decisões e documentos

- **ADR-013** — endpoint aninhado, cursor exclusivo, `desc` default, `asc` auditoria, payload
  com `entryId`/`type`/`balanceAfter`/`counterpartyId`/`createdAt` e read model direto no
  kernel. Sem tabela materializada e sem migração.
- **ADR-014** — hard delete mantido; conta com lançamentos continua bloqueada; D5 adiado
  com gatilho de produto/auditoria para desativar ou ler conta com histórico.
- **D8** pago pelo cenário Cucumber; **D20** pago pela mesma jornada com extrato; **D9**
  pago pela exposição de `createdAt` no extrato e asserção no IT/e2e.
- **D5** re-gatilhado; **D13** passa a ser o próximo candidato, porque “todos os slices +
  e2e verde” foi atingido.
- **D21** registra a camada de enriquecimento do extrato (nome/descrição da contraparte,
  apresentação e testes unitários), com gatilho na conclusão do projeto core.
- O `pom.xml` alinhou JUnit 6.1.3 e Cucumber 8; o Failsafe inclui `**/*E2E.java`.
- O e2e reutiliza o cleanup `TRUNCATE` do ADR-008 no hook de cenário.

## Limites conhecidos

- `entryId` é global e expõe volume/taxa do ledger; aceito em ADR-013.
- O extrato não inclui saldo atual nem nome da contraparte; saldo atual vem de
  `GET /accounts/{id}` e o nome fica para D21 (camada de enriquecimento).
- Soft delete não existe; conta com histórico não pode ser apagada.
- D13 (arquitetura de erros curada) continua aberto; o slice de extrato só adicionou o
  handler mínimo para query param inválido.
- R10 continua sendo gate humano de publicação.
