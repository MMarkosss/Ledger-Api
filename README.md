# Ledger API

[![CI](https://github.com/MMarkosss/Ledger-Api/actions/workflows/ci.yml/badge.svg)](https://github.com/MMarkosss/Ledger-Api/actions/workflows/ci.yml)

A simplified Pix-style payments ledger built with Spring Boot and PostgreSQL. The project
focuses on the parts that make a ledger trustworthy: account invariants, append-only
entries, idempotent writes, and explicit concurrency behavior.

## What is implemented

- Account creation, lookup, listing, rename and deletion.
- Deposits through `POST /accounts/{id}/deposits`.
- Transfers through `POST /transfers`, with deterministic pessimistic locking and
  retryable conflict handling.
- Statements through `GET /accounts/{id}/statements`, with cursor pagination in both
  human (`desc`) and audit (`asc`) order.
- Atomic `transaction` + `ledger_entry` writes.
- Idempotency via the `Idempotency-Key` header.
- Optimistic locking for deposits; deterministic pessimistic locking for transfers.
- Domain errors mapped to explicit HTTP statuses with request correlation via
  `X-Request-Id`/`traceId`.
- Flyway migrations, Testcontainers-backed integration tests and a Cucumber e2e journey.

## Stack

- Java 25
- Spring Boot 4
- Spring Data JPA / Hibernate
- PostgreSQL 18
- Flyway
- Testcontainers
- Cucumber (e2e)
- Maven Wrapper

## Run locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`. Swagger UI is at
`http://localhost:8080/swagger-ui.html`; health and metrics are exposed through Actuator.

The local compose credentials are `ledger` / `ledger` / `ledger` (database / user /
password). They are development-only values.

## API

### Accounts

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/accounts` | Create an account |
| `GET` | `/accounts` | List accounts (paginated) |
| `GET` | `/accounts/{id}` | Read one account |
| `GET` | `/accounts/{id}/statements` | Read the account statement (cursor paginated) |
| `PUT` / `PATCH` | `/accounts/{id}` | Rename the holder |
| `DELETE` | `/accounts/{id}` | Delete an account without ledger entries |

### Deposit

```http
POST /accounts/{id}/deposits
Idempotency-Key: 5f0c2a7e-...
Content-Type: application/json

{ "amount": 100.00 }
```

Response:

```json
{
  "transactionId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e17",
  "balance": 100.0000
}
```

Idempotency contract:

- New key: `201 Created`.
- Same key and same normalized payload: `200 OK`, replaying the stored
  `ledger_entry.balance_after` (not the current balance).
- Same key and different payload: `409 Conflict`.
- Missing or blank `Idempotency-Key`: `400 Bad Request`.
- Invalid amount: `422 Unprocessable Content`.
- Unknown account: `404 Not Found`.

The payload hash is SHA-256 over `accountId|amount`, with the amount normalized through
`stripTrailingZeros().toPlainString()`.

### Transfer

```http
POST /transfers
Idempotency-Key: 5f0c2a7e-...
Content-Type: application/json

{
  "sourceAccountId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e17",
  "destinationAccountId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e18",
  "amount": 30.00
}
```

Response:

```json
{
  "transactionId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e19",
  "sourceBalance": 70.0000,
  "destinationBalance": 30.0000
}
```

Contract:

- New key: `201 Created`.
- Same key and same normalized payload: `200 OK`; replay returns the stored
  `sourceBalance`/`destinationBalance` from the two ledger legs.
- Same key with different payload or transaction type: `409 Conflict` with
  `code=IDEMPOTENCY_CONFLICT`.
- Missing or blank `Idempotency-Key`: `400 Bad Request`.
- Unknown source or destination account: `404 Not Found`.
- Same account, invalid amount or insufficient balance: `422 Unprocessable Content`.
- Lock timeout: `409 Conflict` with `code=CONCURRENCY_CONFLICT` and `retryable=true`.

The payload hash is SHA-256 over `sourceAccountId|destinationAccountId|amount`, with the
amount normalized through `stripTrailingZeros().toPlainString()`. The two accounts are
locked with `PESSIMISTIC_WRITE` in UUID order; there is no internal retry. The database
lock timeout is configurable through `LEDGER_TRANSFER_LOCK_TIMEOUT` (default `2s`) and is
applied with `SET LOCAL` inside the transfer transaction, so deposits and reads do not
inherit it.

### Statement

```http
GET /accounts/{id}/statements?limit=20&order=desc
```

Response:

```json
{
  "order": "desc",
  "entries": [
    {
      "entryId": 42,
      "transactionId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e19",
      "type": "TRANSFER",
      "direction": "DEBIT",
      "amount": 30.0000,
      "balanceAfter": 70.0000,
      "counterpartyId": "01a1cc41-14f1-71ed-8948-6b1ad2da2e18",
      "createdAt": "2026-11-24T12:00:00Z"
    }
  ],
  "nextCursor": 42,
  "hasMore": true
}
```

Contract:

- `order=desc` is the default human order; `order=asc` is the audit order.
- Ordering is always by `ledger_entry.id`; `createdAt` is metadata, not the sort key.
- `cursor` is exclusive: `desc` returns `id < cursor`, `asc` returns `id > cursor`.
- `limit` defaults to `20`, minimum `1`, maximum `100`.
- `nextCursor` is the last `entryId` when `hasMore` is true; pass it back as `cursor`.
- Existing account with no entries: `200 OK` with an empty list.
- Unknown account: `404 Not Found`.
- Invalid account id (not a UUID) or invalid `limit`, `cursor` or `order`: `400 Bad Request`.

The endpoint reads the ledger directly; there is no materialized statement table. The
current balance is not included: each line carries its historical `balanceAfter`, and
`GET /accounts/{id}` remains the live balance source. The `entryId` is a global
`BIGSERIAL`, so exposing it also exposes the ledger's volume/rate; that consequence is
accepted in ADR-013.

### Errors

Errors use RFC 9457 `ProblemDetail`:

- `title`, `status`, `detail` and `traceId` are always present;
- `code` and `retryable` are added when the error has a stable contract;
- `X-Request-Id` is accepted when it has a safe format, otherwise generated, and is
  echoed in the response and in `traceId`;
- unknown routes return `404 Recurso não encontrado`;
- `IDEMPOTENCY_CONFLICT` and `CONCURRENCY_CONFLICT` keep their existing `409` contract;
- optimistic conflicts that escape retries, including a concurrent rename, return
  `409 CONCURRENCY_CONFLICT` with `retryable=true`;
- unexpected failures return `500 Erro interno` with a static message; `message`,
  `stackTrace` and exception details are never included in the response.

## Design decisions

The full records live in [`docs/decisions`](docs/decisions):

- **Account invariants** — `credit`, `debit` and `rename` live on the entity; DTO
  validation is the format gate and the database CHECK is the floor.
- **Account and ledger as separate features** — `Account` lives in `features/accounts`;
  `LedgerEntry` and `Transaction` live in `features/ledger`. The account-to-ledger
  dependency is explicit: `AccountService` uses `LedgerEntryRepository` for the deletion
  pre-check, and the deposit and transfer use cases orchestrate `Account` + `LedgerEntry`
  in one transaction. `balance_after` is a snapshot supplied by the use case, not a
  cross-aggregate invariant.
- **Transfer pairing** — `TransferPair` is a package-private value object local to
  `features.transfers`. It mutates both accounts and creates the two ledger legs with the
  post-mutation snapshots; it is not a central posting service.
- **Statement read model** — `features.statements` reads `ledger_entry` directly by
  account, with a cursor on `id`; `desc` is the human default and `asc` is the audit
  order. No materialized statement table is introduced.
- **Account lifecycle** — `DELETE` remains a hard delete and is blocked when the account
  has ledger entries. Soft delete is deferred; the trigger is a product/audit requirement
  to deactivate or read an account after removal (ADR-014).
- **Entry id exposure** — `ledger_entry.id` is a global `BIGSERIAL`. Exposing it as
  `entryId`/cursor reveals the ledger's volume and rate, not just one account's volume;
  this is accepted as the price of strict audit ordering (ADR-013).
- **Append-only ledger** — `LedgerEntry` has no public builder; creation uses validated
  `credit` or `debit` factories, and a database trigger rejects `UPDATE`/`DELETE`.
  Integration tests clear the ledger with `TRUNCATE` (no `CASCADE`).
- **IDs** — `account` and `transaction` use app-side UUIDv7; `ledger_entry` uses
  `BIGSERIAL` because strict statement ordering is worth more than batching there.
  The PostgreSQL `DEFAULT uuidv7()` remains a floor for non-app writers.
- **Error contract** — feature exceptions extend `DomainException` with status, title,
  optional code/retryable; one advice method materializes `ProblemDetail`, and
  `TraceIdFilter` correlates the request via `X-Request-Id`/`traceId` (ADR-015).
- **Constraint translation** — the service translates the constraint it knows
  (`23505` / `23503` plus constraint name); the generic advice logs the stack and returns
  a static response.
- **DELETE arbitration** — the foreign key is the real arbiter for an account with ledger
  entries; `AccountService` owns the policy with an explicit ledger dependency, and the
  pre-check only improves the common message.
- **Idempotency** — the unique key is global; deposit and transfer share the
  `INSERT ... ON CONFLICT (idempotency_key) DO NOTHING` plus replay contract.
- **Concurrency** — deposits use the account `@Version` and retry on an optimistic
  conflict. Transfers use a deterministic pessimistic lock order, a transaction-scoped
  database lock timeout, and map that timeout to `409 CONCURRENCY_CONFLICT` with
  `retryable=true`.

Process and slice history are documented in [`PROCESS.md`](PROCESS.md),
[`TODO.md`](TODO.md) and [`docs/history`](docs/history).

## Tests

```bash
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be available. The suite is split
between domain/unit tests, HTTP contract tests and one Cucumber e2e journey
(create → deposit → idempotent transfer → both statements, with pair reconciliation,
`balance_after` chain continuity and final balance check). The target pyramid is roughly
70% unit / 20% integration / 10% e2e; the live distribution is tracked in `TODO.md`.
