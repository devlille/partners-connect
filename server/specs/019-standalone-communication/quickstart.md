# Quickstart: Schedule Standalone Communication

**Branch**: `019-standalone-communication`

## Prerequisites

- JDK 21 (Amazon Corretto) installed
- Docker running (for local PostgreSQL)
- `server/` as working directory for all commands

## Running Locally

```bash
# Start PostgreSQL
docker-compose up -d

# Run application (port 8080)
./gradlew run --no-daemon
```

The two new migrations run automatically on startup:
1. `20260316_create_communication_plans_table` — creates `communication_plans` table
2. `20260316_migrate_partnership_communications` — copies existing scheduled dates from `partnerships`

## Running Tests

```bash
# All tests (includes new contract + integration tests)
./gradlew test --no-daemon

# Single test class
./gradlew test --no-daemon --tests "*.CommunicationPlanRoutesTest"

# With coverage report
./gradlew test jacocoTestReport --no-daemon
```

## Linting & Static Analysis

```bash
# Check — run before every commit
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon

# Auto-fix formatting
./gradlew ktlintFormat --no-daemon
```

## Full Validation (mirrors CI)

```bash
npm install && npm run validate   # OpenAPI schema validation
./gradlew check --no-daemon       # build + test + ktlint + detekt
```

---

## API Examples (curl)

### Create a standalone communication

```bash
curl -X POST "http://localhost:8080/orgs/{orgId}/events/{eventId}/communication-plan" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Welcome sponsors email",
    "scheduled_date": "2026-06-15T09:00:00",
    "description": "General welcome message to all confirmed sponsors."
  }'
# → 201 Created
```

### Update a communication plan entry

```bash
curl -X PUT "http://localhost:8080/orgs/{orgId}/events/{eventId}/communication-plan/{entryId}" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated title",
    "scheduled_date": "2026-06-20T10:00:00"
  }'
# → 200 OK
```

### Delete a communication plan entry

```bash
curl -X DELETE "http://localhost:8080/orgs/{orgId}/events/{eventId}/communication-plan/{entryId}" \
  -H "Authorization: Bearer <token>"
# → 204 No Content
```

### List communication planning view

```bash
curl "http://localhost:8080/orgs/{orgId}/events/{eventId}/communication" \
  -H "Authorization: Bearer <token>"
# → 200 OK — now sourced from communication_plans table
```

---

## New Files Created by This Feature

| File | Purpose |
|------|---------|
| `partnership/infrastructure/db/CommunicationPlansTable.kt` | DB schema |
| `partnership/infrastructure/db/CommunicationPlanEntity.kt` | ORM entity |
| `partnership/domain/CommunicationPlanRepository.kt` | Interface |
| `partnership/application/CommunicationPlanRepositoryExposed.kt` | Implementation |
| `partnership/infrastructure/api/CommunicationPlanRoutes.kt` | POST/PUT/DELETE routes |
| `internal/migrations/versions/CreateCommunicationPlansTableMigration.kt` | DDL migration |
| `internal/migrations/versions/MigratePartnershipCommunicationsMigration.kt` | DML migration |
| `resources/schemas/communication_plan_request.schema.json` | JSON schema for request validation |
| `test/.../factories/CommunicationPlan.factory.kt` | Test factory |
| `test/.../api/CommunicationPlanRoutePostTest.kt` | Contract test (POST) |
| `test/.../api/CommunicationPlanRoutePutTest.kt` | Contract test (PUT) |
| `test/.../api/CommunicationPlanRouteDeleteTest.kt` | Contract test (DELETE) |
| `test/.../CommunicationPlanRoutesTest.kt` | Integration test |

## Files Modified by This Feature

| File | Change |
|------|--------|
| `partnership/domain/CommunicationPlan.kt` | `CommunicationItem` updated (new fields, nullable companyName) |
| `partnership/application/PartnershipCommunicationRepositoryExposed.kt` | Re-sourced to new table |
| `partnership/infrastructure/bindings/PartnershipModule.kt` | Add Koin binding |
| `partnership/infrastructure/api/PartnershipRoutes.kt` | Register new routes |
| `internal/migrations/MigrationRegistry.kt` | Append two new migrations |
| `test/.../EventCommunicationPlanRouteGetTest.kt` | Update test setup to new factory |
| `resources/openapi.yaml` | Add new operations + updated CommunicationItem schema |

---

## Troubleshooting

**Migration fails on startup**: Check that `CreateCommunicationPlansTableMigration` runs before `MigratePartnershipCommunicationsMigration` in `MigrationRegistry.allMigrations`. The table must exist before the DML migration inserts rows.

**Test: `NotFoundException` on communication plan queries**: Ensure `insertMockedCommunicationPlan` is called inside the `transaction {}` block and that the `eventId` passed matches the seeded event.

**ktlint failure after adding `updatedAt`**: Run `./gradlew ktlintFormat --no-daemon` to fix trailing blank lines or import ordering automatically.

**OpenAPI validation fails**: Run `npm run validate` from `server/` to see which field is missing from `openapi.yaml`. Most likely `CommunicationItem.company_name` needs to be updated to `nullable: true` / union type.
