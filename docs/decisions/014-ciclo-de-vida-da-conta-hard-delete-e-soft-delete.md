# ADR-014 — Ciclo de vida da conta: hard delete e soft delete adiado

**Status:** accepted
**Data:** 2026-11-24
**Origem:** D5 + abertura do slice `statements`

> O extrato lê lançamentos por conta; ele não exige que uma conta com histórico possa ser
> apagada. Soft delete é uma decisão de ciclo de vida da conta, não do read model.

## Contexto

O `DELETE /accounts/{id}` atual é um hard delete e devolve `409` quando a conta tem
lançamentos. A regra é sustentada por duas camadas: o pré-check
`LedgerEntryRepository.existsByAccountId` e a FK de `ledger_entry.account_id`, que é o
árbitro real em caso de corrida.

D5 foi registrado com gatilho “extrato”. O raciocínio era que o extrato precisaria
preservar a conta para auditoria. Porém, o desenho atual já impede apagar conta com
histórico: uma conta sem lançamentos não aparece em extrato nenhum. O extrato pode ler
`account` + `ledger_entry` sem que soft delete exista.

Soft delete, se implementado, não é uma coluna isolada. Ele exige decidir:

- filtragem em `GET /accounts`, `GET /accounts/{id}` e validações de transferência/depósito;
- unicidade de documento após a “remoção”;
- se uma conta inativa continua aceitando operações ou apenas leitura;
- se o extrato de conta inativa continua acessível e para quem;
- autorização/auditoria para distinguir conta ativa de conta encerrada.

## Decisão

1. **Manter o hard delete atual.** A conta só pode ser apagada quando não tem lançamentos;
   a FK continua sendo o árbitro e o `409` continua sendo o contrato.
2. **Não implementar soft delete no slice `statements`.** O extrato lê o histórico de
   contas que ainda existem; a deleção de conta com histórico continua bloqueada.
3. **Adiar D5 com novo gatilho:** quando houver requisito de produto de desativar/encerrar
   uma conta com histórico, ou quando uma conta removida precisar continuar legível para
   auditoria/atendimento. O gatilho antigo “extrato” deixa de valer.
4. **Se D5 voltar, ele é um slice próprio de ciclo de vida de conta.** A decisão deve
   cobrir schema (`deleted_at` ou status), filtros de leitura, unicidade de documento,
   operações permitidas e acesso ao extrato de conta inativa.

## Consequências

- Nenhuma migração de `account` neste slice.
- `DELETE` continua `204` para conta sem lançamentos e `409` para conta com histórico.
- O extrato não precisa decidir o que fazer com conta apagada, porque esse estado não
  existe no modelo atual.
- D5 permanece aberto, com o gatilho de produto/auditoria descrito acima.
- Se soft delete chegar, o contrato de `GET /accounts/{id}/statements` pode precisar de
  uma regra explícita para conta inativa; isso será decidido no slice de ciclo de vida.

## Alternativas rejeitadas (steelman)

- **Soft delete agora** — permite encerrar conta com histórico sem perder o vínculo do
  extrato, mas amplia o slice para lifecycle inteiro (schema, filtros, unicidade,
  operações e autorização) e não é necessário para ler o extrato atual.
- **Apagar conta com lançamentos em cascata** — destrói a auditabilidade e conflita com
  o ledger append-only; rejeitado.
- **Nunca apagar conta** — mais simples para auditoria, mas remove o contrato de deleção
  já testado e não resolve o caso de conta sem histórico; rejeitado.

## Gatilho de reabertura

- Produto pedir “encerrar conta” mantendo o histórico.
- Auditoria/atendimento precisar consultar conta removida.
- Requisito de privacidade/LGPD exigir anonimização ou remoção lógica de dados pessoais.
