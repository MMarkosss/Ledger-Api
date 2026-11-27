# ADR-013 — Extrato: leitura por conta, cursor e ordem auditável

**Status:** accepted
**Data:** 2026-11-24
**Origem:** slice `statements` (ST0) + decisões de contrato de 2026-11-24

> O extrato nasce no slice `statements` como leitura do kernel do ledger. A transferência
> já grava as duas pernas; o extrato apenas as ordena e as expõe com os snapshots.

## Contexto

O `ledger_entry` é append-only e já tem tudo que o extrato precisa: `id` BIGSERIAL,
`transaction_id`, `account_id`, `direction`, `amount`, `balance_after`, `counterparty_id`
e `created_at`. O índice `(account_id, id DESC)` existe desde o `V1` e o ADR-004 escolheu
esse id justamente para dar ordenação estrita.

Faltava um contrato de leitura por conta. A decisão precisa cobrir:

- paginação estável em uma tabela append-only;
- leitura humana (mais recente primeiro) e leitura de auditoria (mais antiga primeiro);
- sem nova tabela, sem materializar saldo e sem transformar `Account` em aggregate do
  histórico;
- uma jornada e2e que prove create → deposit → transfer → statement.

## Decisão

1. **Endpoint aninhado na conta.** `GET /accounts/{accountId}/statements`. O extrato é uma
   leitura da conta; o path deixa a raiz explícita e evita um endpoint global que exigiria
   `accountId` no query string.
2. **Cursor por `ledger_entry.id`.** O parâmetro é `cursor` e é exclusivo:
   - `order=desc` (default): retorna `id < cursor`, do maior para o menor;
   - `order=asc`: retorna `id > cursor`, do menor para o maior.
   `limit` tem default `20`, mínimo `1` e máximo `100`. A ordenação é sempre por `id`;
   `created_at` é metadado, não chave de ordem.
3. **`desc` é o default humano; `asc` é a adição de auditoria.** A resposta devolve
   `order`, `entries`, `nextCursor` e `hasMore`. `nextCursor` é o `id` do último item da
   página quando `hasMore` é verdadeiro; caso contrário, é `null`. O cliente devolve esse
   valor em `cursor` para buscar a próxima página.
4. **Payload do lançamento.** Cada item expõe `entryId`, `transactionId`, `type`
   (`DEPOSIT`/`TRANSFER`, lido de `transaction`), `direction` (`CREDIT`/`DEBIT`), `amount`,
   `balanceAfter`, `counterpartyId` e `createdAt`. O saldo atual não entra no extrato: cada
   linha já tem seu snapshot e `GET /accounts/{id}` continua sendo a fonte do saldo vivo.
5. **Conta existente sem lançamentos é `200` com lista vazia.** Conta inexistente é `404`.
   `limit`, `cursor` e `order` inválidos são `400`. A validação não pode depender de
   `ConstraintViolationException` cair no handler genérico.
6. **Read model direto, sem tabela nova.** A fatia `statements` consulta o kernel com
   `Slice<LedgerEntry>` em `LedgerEntryRepository` e resolve o tipo em lote com
   `TransactionRepository`. Não há view materializada, aggregate novo nem migração.
7. **BDD/e2e com Cucumber no extrato.** Uma única jornada cobre D8 e D20: criar contas,
   depositar, transferir com replay idempotente e conciliar os dois extratos, comprovando
   ordem, espelhamento C/D (mesmo `transactionId`/`amount`, direções opostas e
   contrapartes cruzadas), continuidade da cadeia de `balanceAfter`, saldo final igual ao
   `balanceAfter` mais recente, `createdAt` e ausência de dupla transferência.

## Consequências

- `features.statements` recebe controller, service, query/ordem, read model e DTOs.
- O kernel ganha leituras paginadas por conta; a lógica de contrato fica na fatia.
- O e2e passa a fazer parte da suíte e o Failsafe inclui `**/*E2E.java`.
- O `entryId` exposto é um `BIGSERIAL` **global**. Isso revela o volume e a taxa de
  lançamentos do ledger, não apenas o volume da conta. A consequência é aceita porque o
  cursor é a própria chave de ordenação auditável; se ocultar volume virar requisito, o
  cursor pode virar opaco sem mudar a ordem interna.
- Não há migração de banco nem mudança nas tabelas.
- D5 (soft delete) não é pré-requisito do extrato; a decisão de ciclo de vida da conta fica
  no ADR-014.
- Nome/descrição da contraparte e apresentação para UI ficam fora deste contrato; D21
  registra a camada de enriquecimento com gatilho na conclusão do core.

## Alternativas rejeitadas (steelman)

- **`GET /statements?accountId=...`** — menos aninhamento e mais fácil para um futuro
  extrato multi-conta, mas esconde a raiz e duplica a validação de conta no query string.
- **Offset/`Page`** — alinhado ao `GET /accounts`, mas instável em ledger append-only:
  novas linhas deslocam páginas e podem duplicar ou pular lançamentos.
- **Somente `desc`** — é o caso humano e reduz superfície, mas auditoria e e2e percorrem
  o histórico de trás para frente com mais trabalho; `asc` é uma adição pequena e usa o
  mesmo índice.
- **Cursor opaco** — esconde o volume global, mas adiciona codificação/decodificação e
  esconde a chave de ordenação sem necessidade atual.
- **View/tabela materializada de extrato** — melhoraria leitura em escala, mas duplicaria
  o ledger e exigiria sincronização; o volume atual não justifica. O gatilho é performance
  medida, não antecipação.

## Gatilho de reabertura

- O volume global exposto por `entryId` virar requisito de segurança/privacidade; nesse
  caso avaliar cursor opaco.
- O custo de paginação por cursor ou a leitura de extratos muito grandes exigir view
  materializada/particionamento.
- A necessidade de extrato multi-conta ou filtros por tipo/direção mudar o contrato.
