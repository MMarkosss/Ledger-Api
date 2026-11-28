# Slice `errors` — D13/D23 — 2026-11-27

> D13 cura a arquitetura de erros; D23 é absorvido no mesmo movimento. O gatilho de D13
> estava satisfeito: todos os slices funcionais encerrados e a jornada e2e verde.

## Contexto

O advice tinha um `@ExceptionHandler` por exceção de feature. Status, título e mensagem
eram repetidos em cada método; o fallback era o único caminho para erros não previstos e
não havia correlação entre a resposta e o log. Rota inexistente não tinha um contrato
explícito, e um `ObjectOptimisticLockingFailureException` fora dos retries de depósito
(ex.: rename concorrente) podia virar `500`.

## Concluídos

- **E1 — `DomainException`**: exceções de feature carregam `HttpStatus`, título e mensagem;
  `code`/`retryable` são opcionais. O advice tem um único handler de domínio.
- **E2 — `TraceIdFilter`**: aceita `X-Request-Id` com formato seguro ou gera UUID; propaga
  no header, no MDC e no `traceId` do `ProblemDetail`.
- **E3 — `404` explícito**: `NoResourceFoundException`/`NoHandlerFoundException` retornam
  `404 Recurso não encontrado`.
- **E4 — D23**: `OptimisticLockingFailureException` (inclui
  `ObjectOptimisticLockingFailureException`) entra no contrato de
  `409 CONCURRENCY_CONFLICT` com `retryable=true`.
- **E5 — sem vazamento**: `server.error.include-message=never`,
  `include-stacktrace=never`, `include-exception=false` e
  `include-binding-errors=never`; o fallback responde `500 Erro interno` e loga a stack.
- **E6 — auditoria**: erros de domínio são logados em `warn` com traceId, método, URI,
  status, código e mensagem; o fallback é logado em `error`.

## Verificação

- `./mvnw clean verify` verde: **17 unit/contexto / 51 integração / 1 e2e** (69 total).
- `ApiExceptionHandlerTest` cobre o mapeamento D23 e o fallback estático com traceId.
- `ErrorContractIT` cobre `404` de rota inexistente, propagação de `X-Request-Id`, traceId
  em erro de domínio e em erro de validação.
- A suíte existente preservou títulos, status e códigos (`IDEMPOTENCY_CONFLICT`,
  `CONCURRENCY_CONFLICT`).

## Decisões e documentos

- ADR-015 — arquitetura de erros, correlação e conflito otimista.
- ADR-003 — nota de que a arquitetura curada foi paga pelo ADR-015.
- `README.md` — seção de erros e decisão de contrato.
- `TODO.md` — D13/D23 fechados; próximo foco é o passo 7.

## Limites conhecidos

- O `traceId` é correlação de request, não tracing distribuído.
- Códigos estáveis existem só para idempotência e concorrência; não houve expansão de
  contrato para todos os erros.
- R10 continua sendo gate humano de publicação.
