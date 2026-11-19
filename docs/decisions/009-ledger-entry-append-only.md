# ADR-009 — `LedgerEntry` append-only: construção e persistência

**Status:** accepted (Q1/Q2; amended by [ADR-011](011-dependencia-explicita-accounts-ledger.md) em 2026-11-19)
**Data:** 2026-11-18
**Origem:** polimento do backlog + slice `polish`

> Nota ADR-011: o `LedgerPostingService` foi removido; as factories `credit`/`debit` são
> públicas, mas continuam validadas. O append-only de persistência permanece. O
> `balanceAfter` é um snapshot fornecido pelo use case, não uma invariante cross-aggregate.

## Contexto

O `ledger_entry` já era descrito como append-only, mas a regra não estava fechada:

- `LedgerEntry` tinha `@Builder` público, permitindo criar lançamento com `direction`,
  `amount` e `balanceAfter` arbitrários, sem passar por `Account.credit/debit`;
- `LedgerEntryRepository extends JpaRepository` expunha `delete`/`deleteAll`;
- o banco aceitava `UPDATE` e `DELETE` em `ledger_entry`;
- `@Immutable` sozinho não impede `DELETE` nem SQL cru.

O kernel do ledger não podia depender de `accounts` (ADR-007). Na época, o Java não
conseguia provar sozinho que `Account.credit/debit` foi chamado; a garantia possível era
fechar a construção, proteger a persistência e manter o service da fatia como transaction
script que pareia as duas operações. O ADR-010 tentou resolver isso com um posting service,
mas foi revertido pelo ADR-011: a fatia de depósito voltou a orquestrar conta e ledger, e
as factories precisaram voltar a ser públicas.

## Decisão

1. **Construção explícita:** remover o builder e o construtor público. A criação passa por
   `LedgerEntry.credit(...)` e `LedgerEntry.debit(...)`, públicas para o use case; as
   factories validam IDs, `amount > 0` e `balanceAfter >= 0`. Elas não sabem se o
   `balanceAfter` corresponde ao saldo da conta; essa relação é responsabilidade do
   transaction script.
2. **ORM:** marcar a entidade com `@Immutable` e estreitar o repositório para
   `Repository<LedgerEntry, Long>` com `save` e leituras. `delete`/`deleteAll` deixam de
   existir na API do repositório.
3. **Banco:** `V3__ledger_entry_append_only.sql` instala uma trigger `BEFORE UPDATE OR
   DELETE` que rejeita a operação com SQLSTATE `23514`.
4. **Testes:** `LedgerEntryTest` cobre as factories; `LedgerEntryAppendOnlyIT` prova que
   `UPDATE` e `DELETE` via JDBC são rejeitados.
5. **Limpeza dos ITs:** como `DELETE` em `ledger_entry` é rejeitado, `IntegrationTestSupport`
   passa a usar:
   ```sql
   truncate table ledger_entry, transaction, account restart identity
   ```
   sem `CASCADE`. A lista explícita mantém as FKs visíveis; row triggers não disparam em
   `TRUNCATE`.

O `DepositAttemptService` é o transaction script que pareia `Account.credit` e a criação
do lançamento correspondente (ADR-011).

## Alternativas rejeitadas (steelman)

- **Só remover o builder** — fecha a porta mais óbvia, mas mantém update/delete no banco.
- **Só trigger no banco** — impede mutação persistida, mas permite construir lançamento
  arbitrário no código.
- **`@Immutable` sozinho** — protege updates via Hibernate, não `DELETE` nem SQL cru.
- **Mover a criação para `Account`** — foi rejeitada porque faria `accounts` depender do
  kernel do ledger; o ADR-010 tentou a variante com posting service, mas ela foi revertida
  pelo ADR-011. O depósito orquestra os dois lados explicitamente.
- **Factory package-private + posting service** — foi a solução do ADR-010, mas exigia
  mover `Account` para o ledger. Revertida; o preço é a factory pública e o pareamento por
  convenção.
- **Trigger + `DELETE` na limpeza dos testes** — incompatível; desabilitar a trigger no
  teste mascararia a regra que se quer provar.
- **`TRUNCATE ... CASCADE`** — funciona, mas esconde dependências de FK. A lista explícita
  das três tabelas preserva a honestidade sem precisar de `CASCADE`.

## Consequências

- Correções no ledger passam a ser novos lançamentos; não há update/delete de linha.
- O repositório não oferece caminho de deleção em tempo de compilação.
- A limpeza dos ITs mudou de `DELETE` para `TRUNCATE`; ADR-008 foi atualizado.
- A suíte atual é de 35 testes: 14 unit/contexto, 21 integração, 0 e2e.
- O desvio do alvo 70/20/10 continua registrado no `TODO.md`; o novo IT prova uma
  invariante dura, não um detalhe de biblioteca.
- A trigger é uma barreira contra SQL acidental, não contra o dono da tabela desabilitar a
  trigger deliberadamente. Essa é uma decisão operacional aceita.

## Gatilho de reabertura

- Uma correção/backfill legítimo precisar de `UPDATE`/`DELETE`; nesse caso a operação deve
  ser planejada como migração explícita, não como fluxo normal.
- Uma exigência de remoção física de dados (ex.: LGPD) exigir política própria para o
  ledger.
- O custo da trigger aparecer em escrita ou a estratégia de limpeza dos testes deixar de
  funcionar.
