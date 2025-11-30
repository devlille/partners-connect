# Implementation Plan: Display Partnership-Specific Options

**Branch**: `013-partnership-option-display` | **Date**: November 30, 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-partnership-option-display/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enhance the existing partnership detail endpoint (`GET /events/{eventSlug}/partnerships/{partnershipId}`) to return partnership-specific option data with complete descriptions and pricing breakdown, eliminating the need for separate API calls to fetch pack options and pricing. The enhanced response will include:
- Only required pack options and user-selected optional options (not all pack options)
- Complete formatted descriptions merging option description with selected values using parentheses format
- Full pricing breakdown (pack base price, per-option costs with quantities, total amount)
- All data needed for invoice, quote, and agreement generation in a single response

This change will update the invoice, quote, and agreement endpoints to consume the enhanced partnership detail instead of making separate repository calls, while maintaining the existing public endpoint structure and response envelope (company, event, organization, speakers).

## Technical Context

**Language/Version**: Kotlin with JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed ORM, Koin (DI), kotlinx.serialization  
**Storage**: PostgreSQL with Exposed ORM (H2 in-memory for tests)  
**Testing**: Kotlin test framework with H2 in-memory database for integration tests  
**Target Platform**: JVM server (Ktor application)  
**Project Type**: Backend web application (Ktor REST API)  
**Performance Goals**: Not applicable (performance testing explicitly excluded from implementation phase per constitution)  
**Constraints**: Public endpoint (no authentication), must maintain backward compatibility for company/event/organization/speakers envelope  
**Scale/Scope**: Single endpoint enhancement affecting invoice/quote/agreement generation workflows

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Code Quality Standards
- All code will adhere to ktlint formatting and detekt static analysis
- All new domain models and repository methods will have KDoc documentation
- Kotlin idioms will be followed (sealed classes for option types, extension functions for mapping)

### ✅ Comprehensive Testing Strategy
- Integration tests will achieve minimum 80% coverage via HTTP route testing
- Contract tests will validate enhanced response schema before implementation
- Existing tests will be updated to match new response structure
- Mock factories will be used/created for test data setup
- H2 in-memory database will be used for test isolation

### ✅ Clean Modular Architecture  
- Repository implementations will NOT depend on other repositories
- Notification sending remains in route layer (not applicable for this feature)
- Domain models (PartnershipDetail, PartnershipPack) will be enhanced with new fields
- No new repository dependencies - using existing PartnershipRepository interface

### ✅ API Consistency & User Experience
- Existing public endpoint structure maintained (no breaking changes to envelope)
- Response times not a concern per constitution (performance testing excluded)
- OpenAPI documentation will be updated with new response schema
- JSON schema validation via `call.receive<T>(schema)` not needed (GET endpoint, no request body)

### ✅ Database Schema Standards
- No new tables required - using existing partnership/option relationships
- Existing Exposed entities (PartnershipEntity, PartnershipOptionEntity, SponsoringOptionEntity) will be used
- No schema migrations needed - feature uses existing data structures

### ✅ Authorization Pattern
- Endpoint remains public (no AuthorizedOrganisationPlugin needed)
- Consistent with existing `GET /events/{eventSlug}/partnerships/{partnershipId}` access pattern

### ✅ Exception Handling Pattern
- Will use existing exception types (NotFoundException for missing translations per FR-027)
- StatusPages will handle exception-to-HTTP conversion
- No manual try-catch blocks in routes

### ✅ Parameter Extraction Pattern
- Will use `call.parameters.eventSlug` and `call.parameters.partnershipId` extensions
- Consistent with existing endpoint implementation

### ✅ OpenAPI Configuration Standards
- Will update `openapi.yaml` with enhanced PartnershipDetail schema
- Will create dedicated JSON schema files for new response components
- Will use schema component references (not inline schemas)
- Will run `npm run validate` to ensure zero errors
- Response will use `event_slug` (not `event_id`) per slug vs ID standards

**Gate Status**: ✅ ALL GATES PASSED - No violations, no complexity tracking needed

## Project Structure

### Documentation (this feature)

```text
specs/013-partnership-option-display/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── partnership_detail_response.schema.json
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/
├── main/
│   ├── kotlin/fr/devlille/partners/connect/
│   │   ├── partnership/
│   │   │   ├── domain/
│   │   │   │   ├── PartnershipDetail.kt                    # UPDATE: Add currency field
│   │   │   │   ├── PartnershipPack.kt                      # UPDATE: Add options with pricing, totalPrice
│   │   │   │   ├── PartnershipOption.kt                    # NEW: Partnership-specific option with complete description
│   │   │   │   ├── DetailedPartnershipResponse.kt          # UNCHANGED: Envelope structure remains same
│   │   │   │   └── PartnershipRepository.kt                # UPDATE: Enhance getByIdDetailed return type
│   │   │   ├── application/
│   │   │   │   ├── PartnershipRepositoryExposed.kt         # UPDATE: Implement enhanced getByIdDetailed
│   │   │   │   └── mappers/
│   │   │   │       └── PartnershipOptionEntity.ext.kt      # NEW: Map entity to domain with complete description
│   │   │   └── infrastructure/
│   │   │       └── api/PartnershipRoutes.kt                # UNCHANGED: Route logic remains same
│   │   ├── billing/
│   │   │   ├── infrastructure/api/BillingRoutes.kt         # UPDATE: Use partnership detail instead of pricing endpoint
│   │   │   └── application/BillingRepositoryExposed.kt     # UPDATE: Accept enhanced PartnershipDetail
│   │   ├── partnership/
│   │   │   └── application/
│   │   │       └── PartnershipAgreementRepositoryExposed.kt # UPDATE: Use partnership detail instead of separate queries
│   │   └── sponsoring/
│   │       └── application/mappers/
│   │           └── SponsoringOptionEntity.ext.kt            # NEW: Add mapping for partnership option context
│   └── resources/
│       ├── openapi/
│       │   └── openapi.yaml                                # UPDATE: Enhanced partnership detail schema
│       └── schemas/
│           ├── partnership_detail.schema.json              # UPDATE: Add currency field
│           ├── partnership_pack.schema.json                # UPDATE: Add options array, totalPrice
│           └── partnership_option.schema.json              # NEW: Option with complete description & pricing
└── test/
    └── kotlin/fr/devlille/partners/connect/
        ├── partnership/
        │   ├── PartnershipDetailedGetRouteTest.kt          # UPDATE: Test enhanced response structure
        │   └── PartnershipMockFactories.kt                 # UPDATE: Add pack_options to mock data
        ├── billing/
        │   └── BillingRouteTest.kt                         # UPDATE: Test invoice/quote with new partnership detail
        └── agreement/
            └── AgreementRouteTest.kt                       # UPDATE: Test agreement with new partnership detail
```

**Structure Decision**: Backend-only changes to existing Ktor REST API. The frontend structure (`front/`) is not affected by this change as it will automatically receive the enhanced response through the existing API client (`utils/api.ts` generated via Orval from OpenAPI). No mobile apps exist in this project.

## Complexity Tracking

> **No violations identified - this section intentionally left empty**

All constitutional gates passed without violations. No complexity justification needed.

---

## Phase 1: Design & Contracts

**Status**: ✅ COMPLETE

### Artifacts Created

1. **data-model.md** - Complete entity definitions with:
   - Enhanced PartnershipDetail domain model (currency)
   - Enhanced PartnershipPack domain model (requiredOptions, optionalOptions, totalPrice)
   - NEW PartnershipOption sealed class (4 subtypes: Text, Quantitative, Number, Selectable)
   - PartnershipOptionEntity.toDomain() mapper extension
   - Entity relationships and query strategy
   - Validation rules and pricing calculations
   - Complete description format examples

2. **contracts/** - JSON Schema files (OpenAPI 3.1.0 compatible):
   - `partnership_option.schema.json` - Polymorphic schema with oneOf discriminator
   - `partnership_pack.schema.json` - Pack with partnership-specific options array
   - `partnership_detail.schema.json` - Enhanced detail with pricing fields
   - `detailed_partnership_response.schema.json` - Complete envelope structure

3. **quickstart.md** - Developer guide with:
   - Prerequisites and environment setup
   - 5-phase implementation workflow
   - Testing strategy (unit, integration, contract tests)
   - Quality gates (ktlint, detekt, OpenAPI validation)
   - Common issues and solutions
   - Timeline estimate (5-8 hours)

### Key Design Decisions

- **Complete Description Format**: Parentheses separator per research decision #1
  - Example: `"Conference passes (5 attendees)"`
- **Pricing Location**: Repository layer calculations per research decision #2
  - Total = base price + sum(optional option amounts)
- **Backward Compatibility**: Breaking change acceptable (user waived concerns)
  - PartnershipPack.options type changes from SponsoringOption to PartnershipOption
- **Schema Structure**: Polymorphic with type discriminator per research decision #4
  - oneOf with "type" field distinguishes Text/Quantitative/Number/Selectable
- **Error Handling**: ForbiddenException for missing translations per research decision #5
  - Thrown when option translation unavailable for partnership language

### Implementation Guidance

**Entry Point**: Follow `quickstart.md` for step-by-step development workflow.

**Quality Gates** (run before commit):
```bash
cd server
./gradlew ktlintCheck detekt test build --no-daemon
npm run validate  # OpenAPI validation
```

**Test Coverage Targets**:
- Unit tests: 80% minimum (complete description formatting, pricing calculations)
- Integration tests: Partnership detail endpoint, billing routes, agreement routes
- Contract tests: JSON schema validation for all response types

**Estimated Timeline**: 5-8 hours total implementation time.

---

## Phase 2: Task Breakdown

**Status**: ⏸️ PENDING - Run `/speckit.tasks` command to generate tasks.md

Phase 2 will be created by the `/speckit.tasks` command, which generates a detailed task breakdown based on this plan. Do not manually create tasks.md - it will be auto-generated with:
- Specific implementation tasks per file
- Acceptance criteria per task
- Dependency relationships
- Test requirements per module
