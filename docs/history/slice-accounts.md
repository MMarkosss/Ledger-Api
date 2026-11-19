# Slice `accounts` — encerrado em 2026-11-04

> História do slice. As ações e débitos pagos vivem aqui; o estado vivo está no `TODO.md`.
> Este arquivo preserva a numeração original (2(c): nada é apagado/renumerado).

## Concluídos

- [x] **P1** `@RestControllerAdvice` mínimo (`404` / `400` validação / `409` duplicado +
      fallback `500`) — provado em runtime pela rodada T1
- [x] **P2** Remover `balance` dos DTOs de entrada — mass assignment fechado
- [x] **P3** `@NotBlank` em `document` no `WriteReq` — resolvido no mesmo diff de P2;
      `AccGetOneRes` ganhou `id`
- [x] **P4** `document` imutável após `POST` — `AccUpdateReq` só com `holderName`; PUT/PATCH
      não aceitam `document`. Decisão: `AccUpdateReq` trava com `@NotBlank` por enquanto
      (PATCH converge com PUT; parcial real fica para depois)
- [x] **P5** Blindar saldo via `AccountBalanceOperator @Component` (dono único) +
      `applyDelta` package-private + criação sempre `ZERO` — S4 split vetado em favor de
      entity espelho + bean
- [x] **Rodada T1** — T-B: 5 unitários do operator; T-A: 5 de contrato HTTP
      (`AccountContractIT`, Postgres real via `RestTestClient`) — todos verdes em
      `mvn verify`. Infra: failsafe plugin no `pom` + `TestcontainersConfiguration` pública
- [x] **P6** `DELETE` bloqueado quando a conta tem lançamento — `existsLedgerEntryFor`
      nativo + `AccountHasLedgerEntriesException` na feature → `409`
      (`title: Conta com lançamentos`). Teste do guard adiado ao slice de extrato (não se
      testa estado intermediário). `mvn verify` verde (11 testes)
- [x] **P7** `created_at`/`updated_at` na entidade — `Instant` + `@CreationTimestamp`
      (`updatable = false`) / `@UpdateTimestamp`, sem `@Setter`, sem DTO, sem service.
      Teste adiado para a rodada T2. Débito D7 registrado. `mvn verify` verde (11 testes)
- [x] **D3** `AccUpdateRes` enriquecido — record com `id` + `holderName` e controller
      montando a partir da entidade; prova de contrato entra na T2
- [x] **P8 removido do slice** — BDD é iniciativa maior e há pouco material para cenários
      agora; registrado como D8. A remoção é história: P8 existiu, foi cortado, virou D8
- [x] **Rodada T2** — 2 de contrato: PUT e PATCH devolvem `id` (D3) + `holderName`
      atualizado; timestamps cortados por serem comportamento da lib (D9). `mvn verify`
      verde (13 testes: 6 unit/contexto + 7 integração)
- [x] **Debate de arquitetura do slice** — achados A1–A9 registrados (seção própria,
      separados dos débitos); pacote P-A..P-D aprovado
- [x] **P-A** builders removidos + update unificado — `AccountUpdater` único (A2:
      validação no updater, `rename` na entidade vetado por SRP); criação via
      `Account.builder()` no próprio método; `partialupdate` eliminado. `mvn verify`
      verde (13 testes)
- [x] **P-B** transação do Spring + leitura — `@Transactional` de
      `org.springframework` (sai o do jakarta); GETs com `readOnly = true`; escrita
      inalterada. `mvn verify` verde (13 testes)
- [x] **P-C** erros de domínio + superfície da entidade — `AccountNotFoundException` na
      feature (D1); `HttpMessageNotReadableException` → 400 (D2); setters públicos de
      `version`/`document` removidos e `holderName` package-private (A6/D4 parcial); 3
      unitários do `AccountUpdater` (D10) + contrato de corpo ilegível. D11 deferido (sem
      caminho alcançável pela web). `mvn verify` verde (17 testes: 9 unit/contexto + 8
      integração)
- [x] **P-D** registrar achados A3/A7 e confirmar D5–D9 nos gatilhos
- [x] **I1** Instalar Docker engine via repositório oficial (método b, Fedora 44) —
      manual, com você + validado: Docker `29.9.1`, `contextLoads` verde /
      `BUILD SUCCESS` (T-A desbloqueado)

## Achados — debate de arquitetura (A1–A9)

| # | Achado | Estado |
|---|---|---|
| A1 | Builders `AccountWriteBuilder`/`AccountPatchBuilder` são cerimônia (só delegam setters) | pago por P-A (A2) |
| A2 | PUT e PATCH são a mesma operação (mesmo DTO, lógica e resposta) | pago por P-A (A2) |
| A3 | Service devolve entidade; controller mapeia. Risco de lazy/OSIV quando `Account` ganhar relações | deferido — gatilho extrato (read models) |
| A4 | `@Transactional` do jakarta (sem `readOnly`); GETs abrem transação de escrita | pago por P-B |
| A5 | Erros: JSON malformado → 500 (D2); 409 "documento" em violação de FK; `EntityNotFoundException` do jakarta (D1) | D1/D2 pagos por P-C; mensagem de FK acelerada para R1 (era gatilho extrato) |
| A6 | Setters públicos de `version`/`document`/`holderName` | pago por P-C (holderName package-private p/ o updater) |
| A7 | Corrida do DELETE (`exists` + `delete` sem lock); FK é a rede real | design mantido (review confirmou); mensagem corrigida por R1; documentação no README → D16 |
| A8 | POST sem `Location`; `@Version` sem ETag/If-Match | sem ação |
| A9 | Sem e2e e guard do DELETE sem material; PATCH-persistência só se testaria o ORM | sem ação até extrato |

> **Nota de continuidade (2026-11-08):** P-A vetou `rename` na entidade por SRP. O debate
> SRP reverteu o veto pelo critério "quem pede a mudança"; a execução é R5/ADR-001.

## Liquidação de débitos

| Débito | Estado | Nota |
|---|---|---|
| D1 | pago no P-C | exceção de domínio + handler; título inalterado |
| D2 | pago no P-C | corpo ilegível → 400, com teste de contrato |
| D3 | pago na T2 | `id` no PUT/PATCH |
| D4 | parcial | setters removidos; normalização do `holderName` → R5 |
| D10 | pago no P-C | 3 testes do updater; migram para `AccountTest` com R5 |
| D11 | superseded | IAE do saldo deixa de existir com R5/ADR-001; reabre se R5 for revertido |
| D17 (processo) | pago em 2026-11-09 | este split (`PROCESS.md`/ADRs/history/TODO) |

## Término

`P6`-`P7` + `D3` + `T2` + `P-A..P-C` fechados — **slice Accounts encerrado em 2026-11-04**.

Nota: **P-D também está `[x]`** (registro de achados/gatilhos), embora a linha de término
cite apenas P-A..P-C; o history registra os quatro e a discrepância.

Pirâmide ao fim do slice: **8 unit / 8 integração / 0 e2e** (50/50/0); o e2e nasce na
transferência (arbitragem 2(e), 2026-11-09).
