# Tasks: Override Partnership Pricing

**Input**: Design documents from `/specs/017-override-partnership-price/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Exact file paths are included in all descriptions

---

## Phase 1: Setup

**Purpose**: Verify branch and baseline

- [X] T001 Verify baseline build passes on branch `017-override-partnership-price` (`./gradlew build --no-daemon`)
- [X] T002 Copy `contracts/update_partnership_pricing_request.schema.json` to `application/src/main/resources/schemas/update_partnership_pricing_request.schema.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema and entity changes that ALL user stories depend on. Nothing can be implemented until these columns and properties exist.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Add `val packPriceOverride = integer("pack_price_override").nullable()` to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipsTable.kt` (after `organiserId`)
- [X] T004 [P] Add `val priceOverride = integer("price_override").nullable()` to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionsTable.kt` (after `selectedValueId`)
- [X] T005 Add `var packPriceOverride by PartnershipsTable.packPriceOverride` property to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt`
- [X] T006 [P] Add `var priceOverride by PartnershipOptionsTable.priceOverride` property to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipOptionEntity.kt`
- [X] T028 [P] Update test factories to support price override fields: ensure `insertMockedPartnership()` in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/Partnership.factory.kt` accepts `packPriceOverride: Int? = null`; ensure `insertMockedPartnershipOption()` accepts `priceOverride: Int? = null`; follow Constitution II factory pattern: all parameters MUST have defaults, UUID-based unique defaults where applicable, NO transaction management inside factories

**Checkpoint**: DB layer + test factories ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Override Sponsoring Pack Price (Priority: P1) 🎯 MVP

**Goal**: Organisers can store a custom integer price on the pack field of an existing partnership. The override is returned alongside the catalogue price in API responses and replaces the catalogue price for billing.

**Independent Test**: Set a pack price override on a partnership via `PUT /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing` → verify `pack_price_override` is returned on subsequent GET; verify pack catalogue `base_price` is unchanged; verify billing `invoiceItems()` uses the override price.

### Contract Tests for User Story 1 (write FIRST — TDD ⚠️)

> **MUST be written and confirmed FAILING before T007–T015 implementation begins** (Constitution II)

- [X] T029 [US1] Write contract tests in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipPricingRoutePutTest.kt` (naming: `PartnershipPricingRoutePutTest` per constitution convention); cover ALL HTTP status codes: 200 OK with valid pack override; 200 OK on a `pending` or `rejected` partnership (FR-014 — status MUST NOT block override); 400 for negative price; 409 when partnership has no validated sponsoring pack; 401 without auth; 403 with wrong org

### Implementation for User Story 1

- [X] T007 [US1] Verify whether `PartnershipPack` lives in `Partnership.kt` or a separate `PartnershipPack.kt` (plan.md source tree lists it as `PartnershipPack.kt`; confirm actual filename before editing); add `packPriceOverride: Int? = null` field (with `@SerialName("pack_price_override")`) to the data class; update `totalPrice` computation to `(packPriceOverride ?: basePrice) + Σ optionalOptions`
- [X] T008 [US1] Update `SponsoringPackEntity.ext.kt` mapper (`application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/SponsoringPackEntity.ext.kt`): add `packPriceOverride: Int? = null` parameter to `toDomain()`; compute `effectiveBasePrice = packPriceOverride ?: basePrice`; pass `packPriceOverride` to `PartnershipPack` constructor
- [X] T009 [US1] In `PartnershipRepositoryExposed.kt` (`application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`), update all 4 `pack.toDomain(...)` call sites to pass `packPriceOverride = partnership.packPriceOverride`
- [X] T010 [US1] Create new domain file `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/UpdatePartnershipPricing.kt` with `UpdatePartnershipPricing` and `OptionPriceOverride` data classes (as specified in quickstart.md Step 2); include KDoc on both classes documenting the three-way partial-update semantics: field absent = unchanged, field = null = clear override, field = integer = set/replace override (Constitution I: all public types MUST be documented)
- [X] T011 [US1] Add `fun updatePricing(eventSlug: String, partnershipId: UUID, pricing: UpdatePartnershipPricing, packOverridePresent: Boolean): UUID` to `PartnershipRepository` interface in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`; include KDoc documenting the `packOverridePresent` parameter purpose, expected exceptions (`NotFoundException` for missing event/partnership/option, `ConflictException` for missing validated pack), and the partial-update contract (Constitution I)
- [X] T012 [US1] Implement `updatePricing()` in `PartnershipRepositoryExposed.kt` — pack override branch only (validate `packOverridePresent`, check validated pack exists when non-null, set `partnership.packPriceOverride`); when `pricing.optionsPriceOverrides` is non-null at this stage, skip it with an inline comment `// option overrides handled in T018` — this is intentional progressive implementation, not a silent data loss
- [X] T013 [US1] Add `pack_price_override` nullable integer field to `application/src/main/resources/schemas/partnership_pack.schema.json`
- [X] T014 [US1] Add `private fun Route.orgsPartnershipPricingRoutes()` function to `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt` with `PUT` handler; register it in `partnershipRoutes()`; call `receiveText()` ONCE to get raw body, then: **(1)** validate it against the JSON schema (`Schema.from(schema).validate(jsonBody)`) to satisfy Constitution IV requirement for schema-based validation and get automatic 400 on invalid payloads, **(2)** parse `Json.parseToJsonElement(jsonBody).jsonObject` and call `.containsKey("pack_price_override")` for partial-update detection, **(3)** deserialize with `Json.decodeFromString<UpdatePartnershipPricing>(jsonBody)` — do NOT call both `receiveText()` and `receive<T>()` as both consume the request body
- [X] T015 [US1] Update `QontoInvoiceItem.mapper.kt` (`application/src/main/kotlin/fr/devlille/partners/connect/billing/infrastructure/gateways/models/mappers/QontoInvoiceItem.mapper.kt`): replace `pack.basePrice` with `pack.packPriceOverride ?: pack.basePrice` in the pack line item
- [X] T030 [US1] Merge `contracts/openapi-put-pricing.yaml` into `application/src/main/resources/openapi/openapi.yaml`: add `PUT /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing` operation with unique `operationId`, `summary`, `security: - bearerAuth: []`, requestBody referencing `UpdatePartnershipPricingRequest` component, and 200/400/401/403/404/409 responses; register `UpdatePartnershipPricingRequest` in `components/schemas` using `$ref: "schemas/update_partnership_pricing_request.schema.json"` (Constitution IV MUST: all endpoints in openapi.yaml); run `npm run validate`

**Checkpoint**: User Story 1 is fully functional — organisers can set, update, and clear a pack price override; billing uses effective price; OpenAPI spec updated and validated

---

## Phase 4: User Story 2 — Override Prices on One or More Options (Priority: P2)

**Goal**: Organisers can store a custom integer price on individual options attached to an existing partnership. Overrides apply per-option and are independent of the pack override.

**Independent Test**: Set a price override on one option via the pricing endpoint → verify only that option reflects the custom price; other options retain catalogue prices; verify billing uses the override price for that option only.

### Contract Tests for User Story 2 (write FIRST — TDD ⚠️)

> **MUST be written and confirmed FAILING before T016–T020 implementation begins** (Constitution II)

- [X] T031 [US2] Extend `PartnershipPricingRoutePutTest.kt` with US2 contract test cases: 200 OK for valid single option override; 200 OK when overriding multiple options simultaneously; 404 for an option ID not associated with the partnership; 200 OK when clearing an existing option override (set to null)

### Implementation for User Story 2

- [X] T016 [US2] Add `abstract val priceOverride: Int?` (with `@SerialName("price_override")`) to the `PartnershipOption` sealed class in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt`; add `override val priceOverride: Int? = null` concrete field to all 4 subtypes (`TextPartnershipOption`, `QuantitativePartnershipOption`, `NumberPartnershipOption`, `SelectablePartnershipOption`)
- [X] T017 [US2] Update `PartnershipOptionEntity.ext.kt` (`application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/PartnershipOptionEntity.ext.kt`): replace `option.price ?: 0` with `(priceOverride ?: option.price) ?: 0` in all `toX` private functions; for `toSelectablePartnershipOption` replace `totalPrice = selectedVal.price` with `totalPrice = priceOverride ?: selectedVal.price`; pass `priceOverride = priceOverride` to all subtype constructors
- [X] T018 [US2] Complete the option-override branch of `updatePricing()` in `PartnershipRepositoryExposed.kt` (depends on T012 skeleton): remove the `// option overrides handled in T018` placeholder; iterate `pricing.optionsPriceOverrides`, look up each option entity by `(partnershipId, optionId)`, throw `NotFoundException` for unknown options, set `optionEntity.priceOverride`
- [X] T019 [US2] Add `price_override` nullable integer field to `application/src/main/resources/schemas/partnership_option.schema.json`
- [X] T020 [US2] Update `QontoInvoiceItem.mapper.kt` (same file edited in T015 — ensure T015 pack change is already present before applying): replace `option.price` with `option.priceOverride ?: option.price` in the option line item

**Checkpoint**: User Story 2 is fully functional — organisers can set, update, and clear option price overrides independently; billing uses effective option prices

---

## Phase 5: User Story 3 — Combined Pack and Option Price Overrides (Priority: P3)

**Goal**: The pricing endpoint accepts pack and option overrides simultaneously in a single atomic request. This is largely already enabled by the work in US1 + US2; this phase validates atomicity and integration.

**Independent Test**: Submit a `PUT` body containing both `pack_price_override` and `options_price_overrides` for two options → verify all three overrides are persisted in a single transaction; verify a partial failure (invalid option ID) rolls back the entire update.

### Implementation for User Story 3

- [X] T021 [US3] Confirm `updatePricing()` in `PartnershipRepositoryExposed.kt` wraps both the pack update and all option updates inside a single `transaction {}` block (SC-004 + FR-006); if they are in separate transactions, consolidate them; **acceptance criterion**: submitting a body with a valid `pack_price_override` and an invalid option ID must roll back completely — zero DB changes on any failure
- [X] T022 [US3] Add combined-override integration test: submit `PUT` with both `pack_price_override` and `options_price_overrides` with two option entries → assert all three values are stored and returned in `PartnershipDetail`

**Checkpoint**: All three user stories are independently functional and atomicity is guaranteed

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, integration tests, and final validation

- [X] T023 [P] Run `./gradlew ktlintFormat --no-daemon` and fix any remaining formatting violations
- [X] T024 [P] Run `./gradlew detekt --no-daemon` and resolve any static analysis violations
- [X] T025 Write integration tests in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipPricingRoutesTest.kt` (naming convention: `PartnershipPricingRoutesTest`) covering end-to-end workflows: set pack override → billing `invoiceItems()` uses override price; set option override → billing uses override price; clear pack override → billing reverts to catalogue price; omit `pack_price_override` key in body → existing override unchanged (FR-006); set then clear an option override in two sequential requests; **verify no Slack or email notification is triggered for any pricing update** — mock `NotificationRepository` and assert zero calls (FR-015)
- [X] T026 Run full validation: `npm install && npm run validate` (OpenAPI schema), then `./gradlew check --no-daemon`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **blocks all user stories**
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2)
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) — independent of US1 except for `UpdatePartnershipPricing.kt` (T010) and `PartnershipRepository.kt` (T011) created in US1
- **User Story 3 (Phase 5)**: Depends on US1 (Phase 3) and US2 (Phase 4)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

| Story | Depends on | Blocks |
|---|---|---|
| US1 (P1) | Phase 2 complete; T029 written + confirmed failing first | US3 |
| US2 (P2) | Phase 2 complete + T010 + T011 + T012 from US1; T031 written + confirmed failing first | US3 |
| US3 (P3) | US1 + US2 complete | — |

### Within Each User Story (sequential order)

1. Domain model changes (data classes, sealed class, interfaces)
2. Mapper updates (entity → domain)
3. Repository implementation
4. Schema files
5. Route handler
6. Billing mapper

---

## Parallel Opportunities

### Within Phase 2 (Foundational)

```
T003 (PartnershipsTable)      T004 (PartnershipOptionsTable)
T005 (PartnershipEntity)      T006 (PartnershipOptionEntity)
```
T003 + T005 must be sequential (entity references table).  
T004 + T006 must be sequential.  
But T003/T005 pair and T004/T006 pair can run in parallel.

### Within Phase 3 (US1)

```
T029 (contract test, US1)          ← write FIRST, must FAIL initially
T007 (PartnershipPack domain)      ← start implementation here (blocks T008, T009)
T010 (UpdatePartnershipPricing)    ← independent, can run parallel with T007
T008 (SponsoringPackEntity mapper) ← after T007
T009 (RepositoryExposed mappers)   ← after T008
T011 (Repository interface)        ← after T010
T012 (Repository implementation)   ← after T011
T013 (partnership_pack schema)     ← independent [P]
T014 (Route)                       ← after T012
T015 (QontoInvoiceItem mapper)     ← independent [P], after T007
T030 (OpenAPI update)              ← after T014
```

### Within Phase 4 (US2)

```
T031 (contract test, US2)          ← write FIRST, must FAIL initially
T016 (PartnershipOption domain)    ← start implementation here (blocks T017)
T017 (OptionEntity mapper)         ← after T016
T018 (Repository option branch)    ← after T012 (from US1) + T016
T019 (partnership_option schema)   ← independent [P]
T020 (QontoInvoiceItem option)     ← independent [P], after T016
```

---

## Implementation Strategy

**MVP scope**: Complete Phase 1 + Phase 2 + Phase 3 (US1 only) to deliver the minimal viable feature — organisers can override the pack price. This covers the P1 user story with end-to-end billing integration.

**Incremental delivery**:
1. Phase 1 + 2: DB columns (5 minutes, compile-safe)
2. Phase 3 (US1): Pack price override end-to-end (~60 minutes)
3. Phase 4 (US2): Option price overrides (~45 minutes)
4. Phase 5 (US3): Atomicity validation + combined test (~15 minutes)
5. Phase 6: Tests + quality gates (~45 minutes)

**Total task count**: 31 tasks  
**Tasks per user story**: US1 → 11 tasks (T029, T007–T015, T030), US2 → 6 tasks (T031, T016–T020), US3 → 2 tasks (T021–T022)  
**Shared infrastructure**: T028 (test factories in Phase 2)  
**Quality gates**: T023, T024, T025, T026
