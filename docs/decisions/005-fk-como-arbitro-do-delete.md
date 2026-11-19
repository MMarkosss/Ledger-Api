# ADR-005 — FK como árbitro do DELETE

**Status:** accepted
**Data:** 2026-11-09
**Origem:** A7/A16

> Nota ADR-011 (2026-11-19): a política voltou para `AccountService`, agora com dependência
> explícita de `LedgerEntryRepository.existsByAccountId` e de `LedgerConstraintNames`; a FK
> continua sendo o árbitro.

## Contexto

`deleteAcc` faz `exists` + `delete` sem lock: janela TOCTOU entre a checagem e o DELETE.
A FK `ledger_entry.account_id` é a rede real; o dado nunca corrompe.

## Decisão

A janela é aceita. A FK é o árbitro; a mensagem não pode mentir (R1 traduz a FK para
`AccountHasLedgerEntriesException` → 409). Documentação em D16/README.

## Alternativas rejeitadas (steelman)

- **Lock pessimista no DELETE** — custo em toda leitura para proteger uma janela de
  microssegundos.
- **Remover o pré-check** — perde a mensagem amigável no caminho comum.

## Consequências

- R1 cobre a mensagem; D16 documenta "FK como árbitro".
- Nenhuma corrupção possível; só a janela de mensagem.

## Gatilho de reabertura

Se DELETE virar operação de alta contenção.
