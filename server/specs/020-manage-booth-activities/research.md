# Research: Manage Booth Activities

**Phase**: 0 — Research  
**Date**: 2026-03-21  
**Feature**: `020-manage-booth-activities`

---

## Decision 1: Module placement

**Decision**: Place all booth activity code inside the existing `partnership` domain module.

**Rationale**: Activities are tightly scoped to partnerships and require querying `PartnershipOptionsTable`. Keeping them inside `fr.devlille.partners.connect.partnership` avoids a new cross-module dependency and follows the pattern of sub-features like `PartnershipDecisionRoutes.kt`.

**Alternatives considered**:
- Standalone `activities/` module — rejected: adds module overhead for a feature tightly coupled to partnership data.

---

## Decision 2: Route path parameter — `{partnershipId}` not `{partnershipSlug}`

**Decision**: Route path is `/events/{eventSlug}/partnerships/{partnershipId}/activities` using a UUID `{partnershipId}`.

**Rationale**: `WebhookPartnershipPlugin` internally calls `call.parameters.partnershipId` (hardcoded name). Using any other path parameter name would break the plugin. All existing authenticated partnership routes also use `{partnershipId}`. The `StringValues.ext.kt` extension `val StringValues.partnershipId: UUID` is already defined.

**Spec note**: The spec used `{partnershipSlug}` — the correct name is `{partnershipId}`. The spec Assumptions section should be updated accordingly.

**Alternatives considered**:
- `{partnershipSlug}` — rejected: incompatible with `WebhookPartnershipPlugin` and existing extension functions.

---

## Decision 3: Booth eligibility check mechanism

**Decision**: A partnership is booth-eligible when at least one row in `PartnershipOptionsTable` joins via `optionId` to a `SponsoringOptionsTable` row where `selectableDescriptor == SelectableDescriptor.BOOTH`.

**Rationale**: This is the exact schema described in the spec clarifications and confirmed in the database schema. `SelectableDescriptor` is a Kotlin enum with a single value `BOOTH` (serialized as `"booth"`). The check is a simple join query, evaluated per request.

**Query pattern**:
```kotlin
PartnershipOptionsTable
    .innerJoin(SponsoringOptionsTable, { optionId }, { SponsoringOptionsTable.id })
    .selectAll()
    .where { PartnershipOptionsTable.partnershipId eq partnershipId }
    .any { it[SponsoringOptionsTable.selectableDescriptor] == SelectableDescriptor.BOOTH }
```

**Alternatives considered**:
- Boolean flag on PartnershipEntity — rejected: not in the existing schema; would require a migration for existing data.

---

## Decision 4: Webhook integration

**Decision**: Install `WebhookPartnershipPlugin` on the activity mutation route group. No event type, payload, or manual webhook call is required.

**Rationale**: The plugin is a `createRouteScopedPlugin` that hooks into `onCallRespond`. It automatically reads `call.parameters.eventSlug` and `call.parameters.partnershipId` from the route path and calls `webhookRepository.sendWebhooks(eventSlug, partnershipId, WebhookEventType.UPDATED)`. The caller needs only to install the plugin.

**File**: `internal/infrastructure/ktor/WebhookPartnershipPlugin.kt`

**Alternatives considered**:
- Manual `webhookRepository.sendWebhooks()` call in each route handler — rejected: the plugin already handles this, manual calls would duplicate logic.

---

## Decision 5: Activity sorting

**Decision**: Sort by `startTime` ASC nulls last, then by `createdAt` ASC for ties.

**Rationale**: Spec FR-013 requires ascending start time order with null-start activities at the end. Ties broken by creation date as stated in the edge cases section.

**SQL equivalent**: `ORDER BY start_time ASC NULLS LAST, created_at ASC`

---

## Decision 6: No authentication on activity routes

**Decision**: Activity routes are fully public — no `AuthorizedOrganisationPlugin`, no session check.

**Rationale**: Per spec FR-012 and confirmed by the user. Public routes exist in the codebase (e.g., `CompanyRoutes.kt`) without any auth plugin. Activities are managed by partnership owners via a public API.

**Alternatives considered**:
- `AuthorizedOrganisationPlugin` — rejected: partnership owners are not authenticated organiser members.

---

## Key File Locations (confirmed)

| Artifact | Path |
|---|---|
| SponsoringOptionsTable | `sponsoring/infrastructure/db/SponsoringOptionsTable.kt` |
| SelectableDescriptor enum | `sponsoring/domain/OptionDescriptors.kt` |
| PartnershipOptionsTable | `partnership/infrastructure/db/PartnershipOptionsTable.kt` |
| PartnershipsTable | `partnership/infrastructure/db/PartnershipsTable.kt` |
| WebhookPartnershipPlugin | `internal/infrastructure/ktor/WebhookPartnershipPlugin.kt` |
| StringValues extensions | `partnership/infrastructure/api/StringValues.ext.kt` |
| Public route example | `companies/infrastructure/api/CompanyRoutes.kt` |

---

## New Files to Create

| File | Purpose |
|---|---|
| `partnership/infrastructure/db/BoothActivitiesTable.kt` | DB schema for activities |
| `partnership/infrastructure/db/BoothActivityEntity.kt` | ORM entity |
| `partnership/domain/BoothActivity.kt` | Domain model (serializable response DTO) |
| `partnership/domain/BoothActivityRepository.kt` | Repository interface |
| `partnership/application/BoothActivityRepositoryExposed.kt` | Repository implementation |
| `partnership/infrastructure/api/BoothActivityRoutes.kt` | Route handlers |
| `partnership/infrastructure/bindings/BoothActivityModule.kt` | Koin DI binding |
| `resources/schemas/booth_activity_request.schema.json` | JSON schema for create/update |
| `resources/schemas/booth_activity_response.schema.json` | JSON schema for single response |
| `resources/schemas/booth_activity_list_response.schema.json` | JSON schema for list response |
| Test factories | `partnership/factories/BoothActivity.factory.kt` |
| Contract tests | `partnership/infrastructure/api/BoothActivityRoutePostTest.kt`, `...GetTest.kt`, `...PutTest.kt`, `...DeleteTest.kt` |
| Integration test | `partnership/BoothActivityRoutesTest.kt` |
