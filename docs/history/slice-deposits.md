# Slice `deposits` — encerrado em 2026-11-11

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração original é preservada (2(c): nada é apagado/renumerado).

## Concluídos

- [x] **P9** Contrato de idempotência — `Idempotency-Key` obrigatório; SHA-256 de
      `accountId|amount` normalizado; replay `200`, hash diferente `409`, novo `201`;
      status sempre `COMPLETED`.
- [x] **P10** `Transaction` + `LedgerEntry` em `features/ledger` — UUIDv7 na transação,
      `BIGSERIAL` no lançamento, referências UUID cru, `EntryDirection` com converter
      `C`/`D`; estado intermediário coberto por D12 até o P11.
- [x] **P11** `POST /accounts/{id}/deposits` — `Account.credit()`, transação + lançamento
      atômicos, resposta `{transactionId, balance}` e replay conforme P9.
- [x] **P12** Erros do depósito — exceções de domínio em `422`; header
      `Idempotency-Key` ausente/em branco em `400`; valor inválido testado no contrato.
- [x] **P13** Concorrência — `INSERT ... ON CONFLICT (idempotency_key) DO NOTHING` +
      caminho de replay; `@Version` na conta e retry de tentativa em conflito otimista.
- [x] **R1–R9** — pacote de endurecimento pré-P11 e README/CI pagos; R10 permanece gate
      humano de publicação.

## Rodada final

`./mvnw clean verify` verde em 2026-11-11:

- 9 testes unitários/contexto (8 `AccountTest` + 1 contexto);
- 18 testes de integração (8 `AccountContractIT`, 8 `DepositContractIT`,
  2 `DepositConcurrencyIT`);
- 0 e2e — a jornada nasce com transferência + extrato.

Distribuição: 9/18/0. O desvio do alvo 70/20/10 está registrado no `TODO.md`; não foi
declarado convergente.

## Decisões aplicadas

- **ADR-001** — invariantes em `Account`; `credit`/`debit`/`rename` e exceções de domínio.
- **ADR-002** — UUIDv7 gerado pela aplicação; `DEFAULT uuidv7()` como piso para writers
  não-app.
- **ADR-003** — constraint traduzida no service por SQLState + nome; leitura via
  `ConstraintViolationException` do Hibernate, sem depender do driver em compilação.
- **ADR-004** — `ledger_entry.id` é `Long`/`IDENTITY` (`BIGSERIAL`).
- **ADR-005** — a FK é o árbitro real do DELETE; o pré-check só melhora a mensagem.
- **ADR-006** — hash canônico, replay com `ledger_entry.balance_after` armazenado e
  resolução da corrida por `ON CONFLICT` + consulta da vencedora.

## Debate de arquitetura (fechamento)

Pergunta adversarial: *o insert nativo + retry é a única forma de conciliar idempotência e
concorrência sem perder o domínio?*

| Opção | Ideia | Steelman |
|---|---|---|
| **A (mantida)** | `INSERT ... ON CONFLICT DO NOTHING` + replay + retry otimista | Separa a disputa de idempotência (unique) da disputa de saldo (`@Version`); o banco decide a corrida sem lock pessimista. |
| **B** | `saveAndFlush` + captura de unique + retry | Mantém o ciclo de vida JPA e o `@UuidGenerator`; menos SQL nativo. |
| **C** | Lock pessimista/advisory por conta | Elimina retry e o `500` por contenção; paga com serialização e menor throughput. |

A opção A foi mantida porque o unique já é a autoridade da idempotência e o `@Version` já é
a autoridade do saldo; B exigiria traduzir a violação de unique fora do service e C
serializaria depósitos de contas diferentes sem necessidade. O retry é limitado a três
tentativas e não tem backoff.

Achados sem decisão nova:

- Há dois pontos de geração UUIDv7 (anotação da conta e `UuidVersion7Strategy` no insert
  nativo). Ambos usam a mesma estratégia; extrair um adapter só se surgir um terceiro ponto.
- O insert nativo não passa pelo ciclo de vida JPA; `Transaction` permanece como read model
  tipado para o replay. A linha é imutável, então o híbrido é aceitável neste slice.
- Contenção acima de três tentativas ainda vira `500`. Se as métricas mostrarem isso em
  depósitos, o retry vira item próprio com backoff; não há gatilho aberto agora.

## Dívidas

- **D5** soft delete — gatilho: extrato.
- **D6** `existsLedgerEntryFor` → repositório de lançamentos — gatilho: extrato/transfer.
- **D7** `updated_at` autoritativo — gatilho: auditoria.
- **D8** BDD da jornada — gatilho: transferência.
- **D9** timestamps — gatilho: extrato.
- **D13** arquitetura de erros curada — gatilho: todos os slices + e2e verde.
- **D15** PATCH parcial — gatilho: demanda real.

**D14/D16** foram pagos pelo README do R9. **D12** fechou no P11. **D4** fechou com R5.

## Revisão de decisões — 2026-11-14

- Revisão feita depois do debate de arquitetura e antes do fechamento do slice.
- Mapa vivo criado em `docs/decisions/README.md`.
- ADR-001 a ADR-006 revisados: todos `accepted`, nenhum `superseded`.
- ADR-002 e ADR-004 aguardam apenas o gate externo R10; não há divergência de código.
- ADR-003 registra a leitura via `ConstraintViolationException` do Hibernate; D13 continua
  aberto para a arquitetura de erros curada.
- Nenhuma decisão nova ou alterada neste fechamento.
- README, `TODO.md` e esta história sincronizados no mesmo movimento.
