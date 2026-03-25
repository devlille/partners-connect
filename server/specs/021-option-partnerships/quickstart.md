# Quickstart: Option Partnerships

**Feature**: 021-option-partnerships
**Date**: 2026-03-24

## Prerequisites

- JDK 21 (Amazon Corretto)
- Gradle 8.13+
- Node.js (for OpenAPI validation)

## Build & Test

```bash
cd server

# Run all tests (includes the new contract test)
./gradlew test --no-daemon

# Run only sponsoring-related tests
./gradlew test --tests "*Sponsoring*" --no-daemon

# Full quality check (ktlint + detekt + tests)
./gradlew check --no-daemon

# Auto-fix formatting issues
./gradlew ktlintFormat --no-daemon

# Validate OpenAPI spec
npm install && npm run validate
```

## Verify the Feature

### 1. Start the application

```bash
docker-compose up -d   # Starts PostgreSQL
./gradlew run --no-daemon
```

### 2. Get an option detail with partnerships

```bash
curl -s -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/options/{optionId} \
  | jq .
```

### Expected Response Structure

```json
{
  "option": {
    "type": "text",
    "id": "...",
    "translations": { "en": { "language": "en", "name": "...", "description": "..." } },
    "price": 500
  },
  "partnerships": [
    {
      "id": "...",
      "contact": { "display_name": "...", "role": "..." },
      "company_name": "...",
      "event_name": "...",
      "validated_pack_id": "...",
      "language": "en",
      "emails": ["..."],
      "created_at": "..."
    }
  ]
}
```

### 3. Verify the list endpoint is unchanged

```bash
curl -s -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/orgs/{orgSlug}/events/{eventSlug}/options \
  | jq .
```

This should return the same array of `SponsoringOptionWithTranslations` objects — no `partnerships` field.

## Files Changed

| File | Change |
|------|--------|
| `sponsoring/domain/OptionRepository.kt` | Add `getOptionByIdWithPartners()` method |
| `sponsoring/domain/SponsoringOptionDetailWithPartners.kt` | New wrapper data class |
| `sponsoring/application/OptionRepositoryExposed.kt` | Implement new method |
| `sponsoring/infrastructure/api/SponsoringRoutes.kt` | Update `get("/{optionId}")` route |
| `schemas/sponsoring_option_with_partnerships.schema.json` | New JSON schema |
| `openapi/openapi.yaml` | Add GET operation for `/{optionId}` |
| `sponsoring/infrastructure/api/SponsoringOptionRouteGetTest.kt` | New contract test |
