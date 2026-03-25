# Quickstart: Option Usage Count

**Feature**: 022-option-usage-count
**Branch**: `022-option-usage-count`

## Prerequisites

- JDK 21 (Amazon Corretto)
- Node.js (for OpenAPI validation)

## Build & Test

```bash
cd server

# Run all tests
./gradlew test --no-daemon

# Run only the relevant contract test
./gradlew test --no-daemon --tests "fr.devlille.partners.connect.sponsoring.infrastructure.api.SponsoringListOptionRouteGetTest"

# Code quality checks
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon

# Auto-fix formatting
./gradlew ktlintFormat --no-daemon

# Validate OpenAPI spec
npm install && npm run validate

# Full validation (all quality gates)
./gradlew check --no-daemon
```

## Verify the Feature

### 1. Check response shape change

The list endpoint now returns wrapper objects:

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/options
```

**Expected response** (array of wrappers):
```json
[
  {
    "option": {
      "id": "...",
      "type": "text",
      "translations": { ... },
      "price": 500
    },
    "partnership_count": 2
  }
]
```

### 2. Verify count accuracy

1. Create an event with sponsoring options and packs
2. Create partnerships and validate some with packs containing specific options
3. Call the list endpoint — `partnership_count` should match the number of validated partnerships per option
4. Call the detail endpoint (`GET .../options/{optionId}`) — the number of partnerships in the detail should equal the count in the list

### 3. Verify edge cases

- Option not in any pack → `partnership_count: 0`
- Option in pack but no validated partnerships → `partnership_count: 0`
- Declined partnerships → NOT counted
- Multiple packs with same option, one validated partnership → counts once

## Files Changed

| File | Change |
|------|--------|
| `sponsoring/domain/SponsoringOptionWithCount.kt` | NEW: wrapper data class |
| `sponsoring/domain/OptionRepository.kt` | NEW method: `listOptionsWithPartnershipCounts` |
| `sponsoring/application/OptionRepositoryExposed.kt` | Implementation of new method |
| `sponsoring/infrastructure/api/SponsoringRoutes.kt` | List route uses new method |
| `schemas/sponsoring_option_with_count.schema.json` | NEW: JSON schema |
| `openapi/openapi.yaml` | Updated list endpoint response |
| `SponsoringListOptionRouteGetTest.kt` | Updated contract tests |
