# Implementation Plan: Override Partnership Pricing

**Branch**: `017-override-partnership-price` | **Date**: 2026-02-27 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/017-override-partnership-price/spec.md`

## Summary

Organisers need to negotiate custom prices for sponsoring packs and individual options on a per-partnership basis, without mutating shared catalogue data. The implementation extends `PartnershipsTable` and `PartnershipOptionsTable` with nullable `pack_price_override` / `price_override` columns, propagates effective prices through the existing domain mappers, exposes a new org-authenticated `PUT /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing` endpoint, and updates the Qonto billing mapper to use effective (overridden) prices.

## Technical Context

**Language/Version**: Kotlin 1.9.x, JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin  
**Storage**: PostgreSQL (prod) / H2 in-memory (tests) via Exposed ORM  
**Testing**: JUnit 5 via `./gradlew test --no-daemon`; H2 shared-DB pattern (`moduleSharedDb`)  
**Target Platform**: Linux server (JVM)  
**Project Type**: Single project (server only)  
**Performance Goals**: Same as existing endpoints — no special performance constraints  
**Constraints**: Zero ktlint violations; zero detekt violations; ≥ 80% test coverage for new code  
**Scale/Scope**: Affects `partnership` domain only; 2 new DB columns, 1 new endpoint, mapper updates, billing mapper update

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status | Notes |
|---|---|---|---|
| I. Code Quality Standards | Zero ktlint + detekt violations | ✅ PASS | All new code follows existing patterns; `ktlintFormat` run before commit |
| II. Comprehensive Testing | ≥ 80% coverage, contract + integration tests, HTTP route testing | ✅ PASS | `PartnershipPricingRoutePutTest` (contract) + `PartnershipPricingRoutesTest` (integration) planned |
| II. Test Architecture | `moduleSharedDb` pattern, UUID-based factory defaults | ✅ PASS | Tests follow shared-DB pattern per constitution |
| II. Contract Tests | Written BEFORE implementation (TDD), all status codes covered | ✅ PASS | Planned as first step per quickstart.md Step 8 |

No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/017-override-partnership-price/
├── plan.md              ← this file
├── research.md          ← pricing flow analysis, storage and API design decisions
├── data-model.md        ← DB schema, domain model, mapper changes, billing changes
├── quickstart.md        ← step-by-step implementation guide
├── checklists/
│   └── requirements.md
└── contracts/
    ├── openapi-put-pricing.yaml                    ← new endpoint OpenAPI fragment
    └── update_partnership_pricing_request.schema.json  ← JSON schema for request body
```

### Source Code (affected files)

```text
application/src/main/kotlin/fr/devlille/partners/connect/
│
├── partnership/
│   ├── domain/
│   │   ├── PartnershipOption.kt                    ← add priceOverride field to sealed class + subtypes
│   │   ├── PartnershipPack.kt                      ← add packPriceOverride field
│   │   └── UpdatePartnershipPricing.kt             ← NEW: request domain model
│   │   └── PartnershipRepository.kt                ← add updatePricing() method
│   │
│   ├── application/
│   │   ├── PartnershipRepositoryExposed.kt         ← implement updatePricing(); pass packPriceOverride to mappers
│   │   └── mappers/
│   │       ├── PartnershipOptionEntity.ext.kt      ← use (priceOverride ?: option.price) in all price calculations
│   │       └── SponsoringPackEntity.ext.kt         ← accept packPriceOverride param; use effective price for totalPrice
│   │
│   └── infrastructure/
│       ├── api/
│       │   └── PartnershipRoutes.kt                ← add orgsPartnershipPricingRoutes() and register it
│       └── db/
│           ├── PartnershipsTable.kt                ← add pack_price_override column
│           ├── PartnershipEntity.kt                ← add packPriceOverride property
│           ├── PartnershipOptionsTable.kt          ← add price_override column
│           └── PartnershipOptionEntity.kt          ← add priceOverride property
│
└── billing/
    └── infrastructure/
        └── gateways/
            └── models/
                └── mappers/
                    └── QontoInvoiceItem.mapper.kt  ← use effective prices (override ?: catalogue)

application/src/main/resources/
├── schemas/
│   ├── update_partnership_pricing_request.schema.json  ← NEW
│   ├── partnership_pack.schema.json                    ← add pack_price_override field
│   └── partnership_option.schema.json                  ← add price_override field

application/src/test/kotlin/fr/devlille/partners/connect/
└── partnership/
    ├── infrastructure/api/
    │   └── PartnershipPricingRoutePutTest.kt       ← NEW contract test
    └── PartnershipPricingRoutesTest.kt             ← NEW integration test
```

**Structure Decision**: Single project — server only. All changes are within `application/src/main/kotlin` and corresponding test paths.

## Complexity Tracking

No constitution violations. No complexity justification required.
