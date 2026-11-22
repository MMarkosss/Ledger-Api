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
- Atomic `transaction` + `ledger_entry` writes.
- Idempotency via the `Idempotency-Key` header.
- Optimistic locking on the account balance.
- Domain errors mapped to explicit HTTP statuses.
- Flyway migrations and Testcontainers-backed integration tests.

Transfers are implemented at the HTTP integration level. Statements and the end-to-end
journey arrive with the next slice.

## Stack

- Java 25
- Spring Boot 4
- Spring Data JPA / Hibernate
- PostgreSQL 18
- Flyway
- Testcontainers
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

## Design decisions

The full records live in [`docs/decisions`](docs/decisions):

- **Account invariants** — `credit`, `debit` and `rename` live on the entity; DTO
  validation is the format gate and the database CHECK is the floor.
- **Account and ledger as separate features** — `Account` lives in `features/accounts`;
  `LedgerEntry` and `Transaction` live in `features/ledger`. The account-to-ledger
  dependency is explicit: `AccountService` uses `LedgerEntryRepository` for the deletion
  pre-check, and the deposit use case orchestrates `Account.credit` + `LedgerEntry` in one
  transaction. `balance_after` is a snapshot supplied by the use case, not a cross-aggregate
  invariant.
- **Append-only ledger** — `LedgerEntry` has no public builder; creation uses validated
  `credit` or `debit` factories, and a database trigger rejects `UPDATE`/`DELETE`.
  Integration tests clear the ledger with `TRUNCATE` (no `CASCADE`).
- **IDs** — `account` and `transaction` use app-side UUIDv7; `ledger_entry` uses
  `BIGSERIAL` because strict statement ordering is worth more than batching there.
  The PostgreSQL `DEFAULT uuidv7()` remains a floor for non-app writers.
- **Constraint translation** — the service translates the constraint it knows
  (`23505` / `23503` plus constraint name); the generic advice logs the stack and returns
  a static response.
- **DELETE arbitration** — the foreign key is the real arbiter for an account with ledger
  entries; `AccountService` owns the policy with an explicit ledger dependency, and the
  pre-check only improves the common message.
- **Idempotency** — the unique key is global; concurrent duplicates are resolved with
  `INSERT ... ON CONFLICT (idempotency_key) DO NOTHING` followed by replay.
- **Concurrency** — the account `@Version` is the optimistic lock; deposit attempts retry
  on an optimistic conflict.

Process and slice history are documented in [`PROCESS.md`](PROCESS.md),
[`TODO.md`](TODO.md) and [`docs/history`](docs/history).

## Tests

```bash
./mvnw verify
```

Integration tests use Testcontainers, so Docker must be available. The suite is split
between domain/unit tests and HTTP contract tests; the target pyramid is roughly
70% unit / 20% integration / 10% e2e, with the e2e journey arriving with statements.
