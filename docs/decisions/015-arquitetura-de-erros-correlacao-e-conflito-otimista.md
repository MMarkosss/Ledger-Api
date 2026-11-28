# ADR-015 — Arquitetura de erros, correlação e conflito otimista

**Status:** accepted
**Data:** 2026-11-27
**Origem:** D13 + D23 + A11/R2

> Erros de borda precisam ser previsíveis, correlacionáveis e sem vazamento de detalhe
> interno. O domínio declara o contrato; o advice apenas materializa.

## Contexto

O `ApiExceptionHandler` tinha um método por exceção de feature. Isso duplicava status,
título e mensagem, dificultava adicionar erros novos e deixava o fallback como único
caminho para exceções não previstas. Também faltavam:

- `traceId` no corpo e nos logs;
- mapeamento de rota inexistente para `404`;
- configuração explícita para não incluir `message`/`stacktrace` no erro padrão do Spring;
- tratamento de `ObjectOptimisticLockingFailureException` fora dos retries de depósito
  (D23), que hoje podia virar `500` em um rename concorrente.

## Decisão

1. **`DomainException` como contrato do domínio.** Toda exceção de feature carrega
   `HttpStatus`, título e mensagem; `code` e `retryable` são opcionais. O advice tem um
   único `@ExceptionHandler(DomainException.class)` e não faz dispatch por tipo.
2. **`ProblemDetail` como formato único.** Todas as respostas de erro usam RFC 9457, com
   `title`, `status`, `detail`, `traceId` e, quando aplicável, `code` e `retryable`.
3. **`TraceIdFilter` como correlação de request.** O filtro aceita `X-Request-Id` quando
   tem formato seguro, ou gera um UUID; propaga o valor no header da resposta, no MDC e no
   corpo do erro. O traceId é de correlação HTTP, não tracing distribuído.
4. **Rota inexistente é `404`.** `NoResourceFoundException` e `NoHandlerFoundException`
   respondem `404 Recurso não encontrado`.
5. **Conflito otimista fora dos retries é `409` retryable.** `OptimisticLockingFailureException`
   (incluindo `ObjectOptimisticLockingFailureException`) entra no mesmo contrato de
   `CONCURRENCY_CONFLICT` usado pelo lock timeout. O rename concorrente perde a versão e o
   cliente pode reler/retentar; D23 fica absorvido pelo D13.
6. **Sem vazamento no erro padrão.** `server.error.include-message=never`,
   `include-stacktrace=never`, `include-exception=false` e `include-binding-errors=never`.
   O fallback responde sempre `Erro interno` e registra a stack no servidor.
7. **Auditoria de mensagens.** Erros de domínio são logados em `warn` com traceId, método,
   URI, status, código e mensagem; o fallback é logado em `error` com a stack.

## Consequências

- Exceções de feature passam a depender de `shared.web.DomainException`; o handler deixa de
  conhecer cada tipo de erro.
- O `traceId` aparece em todas as respostas de erro e no header `X-Request-Id` de qualquer
  request.
- O contrato existente de títulos, status e códigos (`IDEMPOTENCY_CONFLICT`,
  `CONCURRENCY_CONFLICT`) é preservado.
- Nenhuma migração de banco e nenhuma dependência nova de tracing.
- O D23 é pago no mesmo movimento do D13; o rename concorrente deixa de cair no `500`.

## Alternativas rejeitadas (steelman)

- **Manter um handler por exceção** — é explícito, mas duplica o contrato e faz cada erro
  novo tocar o advice; rejeitado.
- **Adicionar Micrometer Tracing/OpenTelemetry** — daria tracing distribuído real, mas é
  infraestrutura maior do que a necessidade atual de correlação de request; rejeitado.
- **Retry automático no rename** — esconderia o conflito e poderia sobrescrever uma
  alteração concorrente; `409 retryable` deixa a decisão para o cliente; rejeitado.
- **Ecoar `getMessage()` no fallback** — vaza SQL, schema ou detalhe interno; rejeitado.

## Gatilho de reabertura

- Necessidade real de tracing distribuído entre serviços.
- Necessidade de códigos de erro estáveis e documentados para todos os domínios, não só
  idempotência e concorrência.
- Requisito de auditoria que exija persistir erros, não apenas logá-los.
