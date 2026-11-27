# ADR-008 — Isolamento e fatiamento dos testes de integração

**Status:** accepted (TI1–TI4; fechamento 2026-11-17; limpeza atualizada em 2026-11-18)
**Data:** 2026-11-17
**Origem:** itens 4 e 5 do backlog + slice `test-isolation`

## Contexto

Os testes de integração usam `@SpringBootTest(RANDOM_PORT)`, `RestTestClient` e Postgres
real via Testcontainers. O contexto e o banco são compartilhados entre classes; cada
request HTTP comita. O `AccountContractIT` limpava apenas `account` no `@BeforeEach`.
Quando `DepositConcurrencyIT` rodava antes, `ledger_entry` ainda referenciava a conta e o
`deleteAll()` falhava por FK (`ledger_entry_account_id_fkey`) em todos os testes da classe.
O `mvn verify` normal ficava verde apenas porque a ordem padrão executava contas antes de
depósitos.

O problema tinha duas facetas: estado compartilhado sem limpeza completa (item 5) e
classes de contrato largas, misturando vários comportamentos (item 4).

## Decisão

1. **Isolamento de estado:** `IntegrationTestSupport` centraliza as anotações do IT,
   `RestTestClient`, `JdbcTemplate` e uma limpeza `@BeforeEach` com
   `truncate table ledger_entry, transaction, account restart identity` (sem `CASCADE`).
   A lista explícita mantém as FKs visíveis; a ordem não é mais necessária. Após o ADR-009,
   `DELETE` em `ledger_entry` é rejeitado pela trigger append-only, então `TRUNCATE` é a
   operação de teste.
2. **Fatiamento por comportamento:** os contratos viram classes top-level por comportamento,
   com helpers de fatia:
   - contas: `AccountCreationIT`, `AccountReadIT`, `AccountUpdateIT`, `AccountErrorIT`;
   - depósitos: `DepositHappyPathIT`, `DepositIdempotencyIT`, `DepositValidationIT`;
   - concorrência permanece em `DepositConcurrencyIT`.
3. **Sem `@Order`:** a ordem dos testes não é parte do contrato. Se um teste depende da
   ordem, o defeito é de isolamento.
4. **Sem fatiamento de camada neste momento:** o contexto completo continua necessário para
   provar HTTP + serviço + repositório + constraints + idempotência + concorrência. Testes
   `@WebMvcTest`/`@DataJpaTest` não substituem essa prova sem perder cobertura ou duplicar
   testes; não há ganho de tempo medido, porque o custo é startup de contexto/container.

## Alternativas rejeitadas (steelman)

- **`@Order` para fixar `AccountContractIT` antes dos depósitos** — faria o vermelho
  desaparecer, mas manteria o teste dependente de estado e ordem.
- **Limpeza copiada em cada classe** — funciona até alguém esquecer uma tabela ou uma
  classe nova; a base centraliza o contrato de limpeza.
- **`TRUNCATE ... CASCADE`** — limpa mais rápido, mas esconde dependências de FK. A lista
  explícita das três tabelas foi preferida para manter as FKs visíveis sem `CASCADE`.
- **`DELETE` explícito na ordem das FKs** — foi a escolha original do TI1, mas deixou de ser
  possível após o ADR-009: a trigger append-only rejeita `DELETE` em `ledger_entry`.
- **`@DirtiesContext` por classe** — recria contexto/container, não limpa o banco e aumenta
  o tempo.
- **`@WebMvcTest`/`@DataJpaTest` agora** — camada mais estreita é legítima quando prova o
  mesmo comportamento sem mocks tautológicos e substitui um IT. Ainda não há evidência de
  ganho de tempo ou isolamento que justifique o churn.

## Consequências

- A ordem dos ITs deixa de importar; a ordem adversarial (depósitos antes de contas) passa.
- No fechamento TI4, a suíte tinha 29 testes: 11 unit/contexto, 18 integração, 0 e2e. Após
  Q1/Q2 (ADR-009), tem 33: 14/19/0. O fatiamento não muda a distribuição 70/20/10.
- `AccountContractIT` e `DepositContractIT` foram substituídos pelas classes por
  comportamento; os testes e helpers equivalentes foram preservados.
- Todo IT novo deve estender `IntegrationTestSupport` (ou um support de fatia que estenda).
- A limpeza `TRUNCATE` é consequência do ADR-009; `TRUNCATE ... CASCADE` continua rejeitado.
  O e2e Cucumber reutiliza a mesma lista de tabelas no hook de cenário.
- A decisão de não usar `@Order` encerra D18 sem implementação.

## Gatilho de reabertura

- O `mvn verify` ficar lento de forma mensurável e um teste puder ser provado em camada mais
  estreita sem mock tautológico, substituindo (não duplicando) um IT.
- Um comportamento de fatia deixar de exigir HTTP + banco real.
- A limpeza deixar de cobrir uma tabela nova ou um teste precisar de estado acumulado
  entre métodos.
- Uma regra append-only exigir outra operação de limpeza ou um teste precisar de `DELETE`.
