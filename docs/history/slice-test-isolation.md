# Slice `test-isolation` — encerrado em 2026-11-17

> História do slice. As ações pagas vivem aqui; o estado vivo está no `TODO.md`.
> A numeração original é preservada (2(c): nada é apagado/renumerado).

## Concluídos

- [x] **TI1** Base de isolamento — `IntegrationTestSupport` centralizou
  `@SpringBootTest(RANDOM_PORT)`, `@AutoConfigureRestTestClient`, Testcontainers,
  `RestTestClient` e `JdbcTemplate`. A limpeza roda antes de cada teste na ordem FK
  (`ledger_entry → transaction → account`). `AccountContractIT`, `DepositContractIT` e
  `DepositConcurrencyIT` passaram a estender a base.
- [x] **TI2** Fatiar depósitos — `DepositHappyPathIT` (1), `DepositIdempotencyIT` (2),
  `DepositValidationIT` (5); `DepositConcurrencyIT` (2) permaneceu focada em concorrência.
  `DepositITSupport` concentra criação de conta e chamadas HTTP repetidas.
- [x] **TI3** Fatiar contas — `AccountCreationIT` (3), `AccountReadIT` (1),
  `AccountUpdateIT` (3), `AccountErrorIT` (1). `AccountITSupport` concentra criação de
  conta e POST repetido.
- [x] **TI4** Fechar isolamento — `./mvnw clean verify` verde, ordem adversarial verde,
  ADR-008, esta história, mapa de decisões e `TODO.md` sincronizados. D18 encerrado sem
  `@Order`.

## Reprodução do vermelho

Antes do TI1, a ordem `AccountTest → DepositConcurrencyIT → AccountContractIT →
DepositContractIT → IdempotencyHashTest` produzia 20 verdes e 8 falhas. Todas as 8 falhas
do `AccountContractIT` aconteciam no `@BeforeEach`:

```text
delete from account ...
violates foreign key constraint "ledger_entry_account_id_fkey"
Key (id)=(...) is still referenced from table "ledger_entry".
```

O `DepositConcurrencyIT` deixava `ledger_entry`/`transaction`; o `AccountContractIT`
apagava apenas `account`. O `DepositContractIT` seguinte limpava tudo e “consertava” o
banco, o que mascarava a falha. O `mvn verify` padrão ficava verde porque a ordem normal
executava contas antes de depósitos. A falha era determinística dada a ordem, não
flakiness.

## Rodada final

`./mvnw clean verify` verde em 2026-11-17:

- 11 testes unitários/contexto (8 `AccountTest`, 2 `IdempotencyHashTest`, 1 contexto);
- 18 testes de integração:
  - contas: 3 `AccountCreationIT` + 1 `AccountReadIT` + 3 `AccountUpdateIT` +
    1 `AccountErrorIT` = 8;
  - depósitos: 1 `DepositHappyPathIT` + 2 `DepositIdempotencyIT` +
    5 `DepositValidationIT` + 2 `DepositConcurrencyIT` = 10;
- 0 e2e — a jornada continua nascendo com transferência + extrato.

Distribuição: 11/18/0. O fatiamento por comportamento não altera a contagem nem a
distribuição; o desvio do alvo 70/20/10 segue registrado no `TODO.md`.

## Ordem adversarial

Além do `mvn verify` padrão (contas antes de depósitos), os ITs foram executados via JUnit
Platform Launcher na ordem inversa de risco: todos os depósitos primeiro, depois todas as
contas, com os unitários nas pontas. Resultado: 28/28 verdes (`LedgerApiApplicationTests`
não estava na seleção). A limpeza da base tornou a ordem irrelevante.

## Debate de arquitetura (fechamento)

Pergunta adversarial: *o contexto completo é necessário para provar o contrato do slice, ou
um recorte de camada (`@WebMvcTest`/`@DataJpaTest`) bastaria?*

| Opção | Ideia | Steelman |
|---|---|---|
| **A (mantida)** | `@SpringBootTest` + Postgres real para o contrato; fatiar por comportamento | Prova HTTP + serviço + repositório + constraint + idempotência; o custo é startup, não quantidade de classes. |
| **B** | `@WebMvcTest` para validação/status e `@DataJpaTest` para SQL/constraint | Feedback mais rápido e falha mais localizada; legítimo quando substitui um IT, não quando duplica. |
| **C** | Manter classes largas e usar `@Order` | Menos churn; mas esconde dependência de estado e não resolve o vermelho. |

A opção A foi mantida. Os testes atuais provam contratos que atravessam HTTP e banco; um
mock de service ou um recorte de JPA não prova a mesma coisa. O tempo do `mvn verify` é
dominado por startup de contexto/Testcontainers: o primeiro contexto do Surefire levou
~18–21 s, o primeiro do Failsafe ~12–13 s, e as classes seguintes rodaram em décimos de
segundo. Não há evidência de ganho de tempo com fatiamento de camada; além disso,
`@WebMvcTest`/`@DataJpaTest` não contam como unit para a meta 70/20/10.

A opção B fica registrada como reavaliação futura, com gatilho no ADR-008: só promover
quando um teste estreito substituir um IT, preservar a mesma prova e houver ganho medido
de tempo ou isolamento.

## Decisões aplicadas

- **ADR-008** — isolamento por limpeza FK centralizada; fatiamento por comportamento em
  classes top-level; sem `@Order`; sem fatiamento de camada agora.
- **ADR-006** continua válido; a implementação de idempotência agora é provada por
  `DepositIdempotencyIT`/`DepositConcurrencyIT` e pelo caminho feliz de `DepositHappyPathIT`.
- **ADR-007** continua válido; a decisão de kernel não mudou. O fatiamento de testes ocorreu
  depois do fechamento L3.
- **ADR-001** a **ADR-005** não foram alterados.

## Achados sem decisão nova

- A dependência do `AccountContractIT` era de estado de banco, não de imports/dependências
  de código.
- `AccountContractIT` e `DepositContractIT` foram substituídos por classes por
  comportamento; helpers comuns vivem em `AccountITSupport`/`DepositITSupport`.
- O `IntegrationTestSupport` é o único dono da limpeza; um IT novo que não estenda a base
  (ou um support de fatia) fica fora do contrato de isolamento.
- Não houve mudança de contrato HTTP, comportamento, schema ou código de produção.
- D18 foi encerrado sem implementação: `@Order` não é necessário quando a ordem é
  irrelevante por isolamento.

## Dívidas

Nenhuma dívida nova. D5, D6, D7, D8, D9, D13 e D15 mantêm os gatilhos registrados no
`TODO.md`. O fatiamento de camada não vira dívida; fica como gatilho de reabertura do
ADR-008.

## Nota posterior — ADR-009 (2026-11-18)

A limpeza descrita nesta história (`DELETE` explícito na ordem
`ledger_entry → transaction → account`) foi a executada no fechamento TI4. O slice `polish`
depois tornou `ledger_entry` append-only no banco; como a trigger rejeita `DELETE`, o
`IntegrationTestSupport` passou a usar
`truncate table ledger_entry, transaction, account restart identity`, sem `CASCADE`
(ADR-009; ADR-008 atualizado). A ordem dos ITs continua irrelevante.

## Término

Slice `test-isolation` encerrado em 2026-11-17. Transferência e extrato seguem fora deste
slice.
