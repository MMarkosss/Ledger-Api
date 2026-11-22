# ADR-012 — Transferência: duas pernas e lock determinístico

**Status:** accepted
**Data:** 2026-11-21
**Origem:** slice `transfers` (TR0) + decisão A da discussão de concorrência

> Decisão tomada antes do código. A implementação segue em TR1–TR5; o depósito
> continua no modelo otimista do ADR-006/011.

## Contexto

A transferência é a primeira operação que muta **duas contas** na mesma transação:

```text
source.debit(amount)  -> ledger_entry (D, source, counterparty = destination)
destination.credit(amount) -> ledger_entry (C, destination, counterparty = source)
```

O `V1` já suporta `transaction.type = TRANSFER`, `ledger_entry.counterparty_id` e dois
lançamentos por transação. Não há migração nova.

O contrato definido para o slice é:

- `POST /transfers` com `sourceAccountId`, `destinationAccountId` e `amount`;
- `Idempotency-Key` obrigatório, com hash `source|destination|amount` normalizado;
- `201` para nova transferência, `200` para replay, `409` para chave reutilizada com
  payload/tipo diferente;
- resposta com `transactionId`, `sourceBalance` e `destinationBalance`;
- mesma conta, valor inválido e saldo insuficiente em `422`;
- conta de origem ou destino inexistente em `404`.

O risco novo não é a idempotência: ela já está resolvida pelo unique
`transaction.idempotency_key` + `INSERT ... ON CONFLICT DO NOTHING` + replay. O risco é
**concorrência entre duas linhas**. Duas transferências A→B e B→A podem tentar atualizar
as duas contas em ordens diferentes. Sem uma ordem comum, o Postgres pode detectar
deadlock e abortar uma. Retries otimistas sem backoff podem transformar isso em retry
storm/livelock, ou terminar em `500` depois do orçamento de tentativas.

## Decisão

1. **Endpoint top-level.** `POST /transfers`, com as duas contas no body. A operação tem
   duas raízes; aninhar em `/accounts/{sourceId}/transfers` deixaria o path assimétrico.
2. **Duas pernas explícitas.** O transaction script debita a origem e credita o destino,
   grava dois `LedgerEntry` na mesma transação e preenche `counterpartyId` nos dois.
3. **Idempotência preservada.** O hash é `sourceAccountId|destinationAccountId|amount`
   canônico. O `insertIfAbsent` acontece antes da mutação das contas; duplicata
   concorrente bloqueia no unique e cai no replay. O replay lê os dois lançamentos e
   devolve os `balance_after` armazenados, não o saldo atual.
4. **Lock determinístico (opção A).** As duas contas são carregadas com
   `PESSIMISTIC_WRITE` em ordem determinística de UUID. Com a ordem comum, A→B e B→A não
   formam ciclo de locks. A transferência não tem retry interno; a contenção vira espera,
   não spin.
5. **Timeout explícito.** O lock tem timeout curto e configurável. O timeout é mapeado
   para um `409` retryable, com código distinto do `409` de idempotência
   (`CONCURRENCY_CONFLICT` / `retryable=true`, por exemplo). O caminho comum continua
   sendo `201`; o conflito é exceção operacional, não fluxo normal.
6. **Depósito não muda.** O depósito continua otimista com `@Version` e retry curto. A
   diferença é justificada: depósito toca uma linha; transferência toca duas e precisa de
   ordem de lock. Não é inconsistência acidental.
7. **Sem schema novo.** `TRANSFER`, `counterparty_id` e o índice por `transaction_id` já
   existem. A mudança é de aplicação e de contrato.

## Entradas adversariais

### A — lock pessimista determinístico (adotada)

Carregar as duas contas com `PESSIMISTIC_WRITE` ordenadas por UUID, sem retry interno.

**Steelman:** é a única opção que elimina o deadlock de duas linhas por construção, sem
depender de sorte de flush nem de repetição. A transferência é curta; o lock dura poucos
comandos. O cliente não precisa conhecer contenção para o caso comum.

**Custo:** transferências sobre contas quentes formam fila; um lock timeout precisa ser
configurado e mapeado. A espera é real, mas é limitada e explícita.

### B1 — otimista sem retry + conflito retryable

Uma única tentativa com `@Version`; `OptimisticLockingFailureException` e deadlock viram
um `409` retryable para o cliente.

**Steelman:** mantém o mesmo modelo do depósito, não adiciona lock pessimista e evita
retry no servidor. O cliente decide quando repetir, com a mesma `Idempotency-Key`.

**Por que é mais fraca:** não elimina o deadlock; apenas o converte em erro. O progresso
passa a depender do cliente, o contrato HTTP ganha um caminho de conflito que precisa ser
distinguido do conflito de idempotência, e o teste de direções opostas passa a aceitar
`201/409`. É defensável se conflito for parte explícita do contrato, mas é mais superfície
e menos garantia que A.

### B2 — otimista com retry

Repetir a tentativa no servidor, como o depósito faz hoje, até um limite.

**Steelman:** o cliente vê menos conflitos e o código reutiliza o padrão de
`DepositService`/`DepositAttemptService`.

**Por que é mais fraca:** não elimina o deadlock; duas linhas continuam podendo ser
atualizadas em ordens diferentes. Retries sem backoff/jitter aumentam a chance de os dois
lados colidirem de novo, caracterizando retry storm/livelock. Com orçamento finito, o
resultado final pode ser `500`. Copiar o retry de uma linha para duas linhas aumenta a
complexidade sem resolver o problema central.

## Consequências

- `features/transfers` recebe controller, service, DTOs e resultado; `features/ledger`
  continua sendo o kernel de `Transaction`/`LedgerEntry`.
- `AccountRepository` ganha uma leitura com lock ordenado, usada apenas pela transferência.
- `ApiExceptionHandler` ganha o erro de mesma conta (`422`) e o conflito de concorrência
  retryable (`409`, código distinto).
- O lock timeout é aplicado via `spring.datasource.hikari.connection-init-sql` com a
  propriedade `ledger.transfer.lock-timeout` (default `2s`, configurável por
  `LEDGER_TRANSFER_LOCK_TIMEOUT`); o `409` de concorrência usa
  `code=CONCURRENCY_CONFLICT` e `retryable=true`, distinto do
  `IDEMPOTENCY_CONFLICT`.
- TR1–TR4 implementados; TR5 fecha com o e2e, o D19 (teste unitário do pareamento) e a
  documentação.
- A suíte sai de 35 testes e ganha a primeira jornada e2e; a distribuição será medida no
  fechamento, sem declarar convergência.
- Não há migração de banco nem mudança nas tabelas.
- D8 não é pago por esta ADR: o e2e da transferência não é BDD; o gatilho de D8 foi
  re-triggerado para o extrato.

## Gatilho de reabertura

- A taxa de lock timeout ou a fila em contas quentes indicar que a espera pessimista é
  pior que um contrato retryable; nesse caso reavaliar B1, com o conflito explicitamente
  documentado.
- O volume de transferências crescer e a serialização por conta virar gargalo; nesse caso
  medir antes de trocar a estratégia.
- A orquestração conta + lançamento se repetir de forma custosa em depósito, transferência
  e extrato; nesse caso o gatilho do ADR-011 (application service na fatia) é reavaliado,
  sem mover `Account` para o ledger.
