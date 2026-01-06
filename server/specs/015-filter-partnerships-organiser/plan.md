# Implementation Plan: Filter Partnerships by Assigned Organiser

**Branch**: `015-filter-partnerships-organiser` | **Date**: December 29, 2025 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/015-filter-partnerships-organiser/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Add `filter[organiser]` query parameter to partnership list endpoint for filtering by assigned organiser email. Enhance PaginatedResponse with metadata containing available filters (including organisers list with email/displayName) and sorts arrays. Apply same organiser filter to email partnerships endpoint for targeted bulk communications. Organiser information already exists in partnership responses.

## Technical Context

**Language/Version**: Kotlin 2.1.21 with JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 3.2.0, Exposed 1.0.0-beta-2, kotlinx.serialization 1.7.3, Koin 4.1.0  
**Storage**: PostgreSQL (H2 2.2.224 in-memory for tests)  
**Testing**: JUnit Platform with Ktor test server, 80% coverage minimum  
**Target Platform**: Linux server (Docker container), Port 8080  
**Project Type**: REST API server (Ktor backend)  
**Performance Goals**: Sub-2-second response time for partnership list with metadata (up to 1000 partnerships, ~10-100 organisation members)  
**Constraints**: Backwards compatible PaginatedResponse, metadata always included, filter combines with existing filters using AND logic  
**Scale/Scope**: Single endpoint modification (list + email), PaginatedResponse model enhancement, organisation member query for metadata

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Code Quality Standards ✅ PASS
- **ktlint/detekt**: Will apply to all new code (routes, models, tests)
- **Documentation**: All new public APIs will include KDoc
- **No NEEDS CLARIFICATION**: All resolved in spec clarification phase

### II. Comprehensive Testing Strategy ✅ PASS
- **Contract Tests Required**: Yes - `PartnershipListRouteGetTest` (test filter parameter, metadata structure, all status codes)
- **Integration Tests Required**: Yes - `PartnershipOrganiserFilterRoutesTest` (test filter logic with existing filters)
- **Shared Database Pattern**: Will use `moduleSharedDb(userId)` with UUID-based factories
- **80% Coverage Target**: Achievable - simple filter logic + metadata population
- **Test Structure**: Contract tests in `infrastructure.api`, integration tests in root `partnership` package

### III. Clean Modular Architecture ✅ PASS
- **No New Modules**: Extends existing `partnership/` domain
- **Repository Separation**: Will NOT add repository dependencies - route layer orchestrates `PartnershipRepository` + `UserRepository` for metadata
- **Notification Pattern**: Email endpoint already follows correct pattern (notifications in routes)
- **Database Schema**: Reuses existing `partnerships.organiser_user_id` FK, no schema changes needed

### IV. API Consistency & User Experience ✅ PASS
- **JSON Schema Required**: Yes - `pagination_metadata.schema.json`, update `partnership_list_response.schema.json`
- **OpenAPI Documentation**: Must update `openapi.yaml` with new filter parameter and pagination metadata structure
- **HTTP Status Codes**: Follows standard (200 for list, 204 for email with no recipients)
- **Error Messages**: Consistent with existing partnership endpoint patterns
- **Response Format**: Extends existing PaginatedResponse (backwards compatible)

### V. Performance & Observability ✅ PASS
- **Database Optimization**: Filter uses existing `organiser_user_id` foreign key index
- **Performance Target**: Sub-2-second for metadata query (organisation members typically 10-100 users)
- **No Performance Testing in Spec**: Correct - functional validation only in quickstart
- **Monitoring**: Existing Ktor metrics cover new endpoints

**Gate Status**: ✅ ALL GATES PASS - Ready for Phase 0 research

---

## Phase 0: Research & Discovery ✅ COMPLETE

**Status**: All unknowns resolved via codebase research

### Research Findings

**Generated**: [research.md](research.md)

**Key Decisions**:
1. **PartnershipFilters Extension**: Add `organiser: String?` field (email address)
2. **PaginatedResponse Enhancement**: Add `metadata: PaginationMetadata?` with filters/sorts
3. **Entity Filter Method**: Add `organiserUserId: UUID?` parameter to `PartnershipEntity.filters()`
4. **Organisation Query**: Create `listEditorsbyOrgId()` for available organisers
5. **Email Resolution**: Use existing `UserEntity.singleUserByEmail()` (case-insensitive)
6. **Repository Pattern**: Build metadata within repository (no cross-repo dependencies)
7. **Performance**: Single transaction, indexed FK, estimated 250-600ms (target: sub-2s)

**Alternatives Considered**:
- **String-based filtering**: Rejected - UUID comparison more efficient, maintains pattern
- **Separate metadata endpoint**: Rejected - FR-005 requires metadata in every response
- **Dynamic filter values for all filters**: Rejected - only organiser has values per FR-010

---

## Phase 1: Design & Contracts ⚙️ IN PROGRESS

**Status**: Data model and contracts complete, quickstart pending

### Design Artifacts

**Generated**:
- [data-model.md](data-model.md) - Complete domain model specification
- [contracts/partnership-list-api.md](contracts/partnership-list-api.md) - List endpoint contract with 7 test scenarios
- [contracts/partnership-email-api.md](contracts/partnership-email-api.md) - Email endpoint contract with 5 test scenarios
- [contracts/schemas.md](contracts/schemas.md) - JSON schema specifications (4 schemas)

**Key Design Decisions**:
1. **FilterValue Generic Fields**: Use `value`/`displayValue` instead of `email`/`displayName` for future extensibility
2. **FilterType Enum**: Created enum with STRING/BOOLEAN values, @SerialName annotations for JSON consistency
3. **PaginationMetadata**: Always populated (nullable for backwards compatibility)
4. **Single Transaction**: Repository builds metadata in same transaction as partnership query
5. **Organisation Permissions**: List all editors regardless of assignment status (per FR-010)
6. **Distinct by Value**: Ensure unique organiser list using `distinctBy { it.value }`

**Data Model Summary**:
- **Modified**: PartnershipFilters (+organiser field), PaginatedResponse (+metadata field)
- **New**: PaginationMetadata, FilterType enum, FilterDefinition, FilterValue
- **No Schema Changes**: Reuses existing `partnerships.organiser_user_id` FK

**Contracts Summary**:
- 7 test scenarios for list endpoint (filter with results, empty, combined filters, case-insensitive, metadata validation, available organisers, unassigned)
- 5 test scenarios for email endpoint (organiser filter, combined filters, no recipients, empty filter, validation)
- JSON schemas for pagination_metadata, filter_definition, filter_value, partnership_list_response

**Pending**: quickstart.md (testing guide), agent context update

---

## Phase 2: Task Breakdown

**Status**: Not started - awaits completion of Phase 1

*Task breakdown will be generated by `/speckit.tasks` command after Phase 1 completion*

---

## Project Structure

### Documentation (this feature)

```text
specs/015-filter-partnerships-organiser/
├── spec.md              # Feature specification (complete, clarified)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 research findings (complete)
├── data-model.md        # Phase 1 domain model (complete)
├── quickstart.md        # Phase 1 testing guide (pending)
├── contracts/           # Phase 1 API contracts (complete)
│   ├── partnership-list-api.md
│   ├── partnership-email-api.md
│   └── schemas.md
└── tasks.md             # Phase 2 task breakdown (pending /speckit.tasks)
```

### Source Code (affected areas)

```text
application/src/
├── main/
│   ├── kotlin/fr/devlille/partners/connect/
│   │   ├── partnership/
│   │   │   ├── domain/
│   │   │   │   └── PartnershipItem.kt           # Extend PartnershipFilters
│   │   │   ├── application/
│   │   │   │   └── PartnershipRepository.kt     # Update listByEvent()
│   │   │   └── infrastructure/
│   │   │       ├── api/
│   │   │       │   └── PartnershipRoutes.kt     # Add organiser filter support
│   │   │       └── db/
│   │   │           └── PartnershipEntity.kt     # Extend filters() method
│   │   ├── users/infrastructure/db/
│   │   │   └── OrganisationPermissionEntity.kt  # Add listEditorsbyOrgId()
│   │   └── internal/infrastructure/api/
│   │       └── PaginatedResponse.kt             # Add metadata field
│   └── resources/
│       ├── openapi.yaml                          # Update with new parameters
│       └── schemas/
│           ├── pagination_metadata.schema.json   # NEW
│           ├── filter_definition.schema.json     # NEW
│           ├── filter_value.schema.json          # NEW
│           └── partnership_list_response.schema.json  # UPDATE
└── test/
    └── kotlin/fr/devlille/partners/connect/
        └── partnership/
            ├── PartnershipOrganiserFilterRoutesTest.kt      # NEW (integration)
            └── infrastructure/api/
                └── PartnershipListRouteGetTest.kt           # UPDATE (contract)
```

**Structure Decision**: Single Kotlin/Ktor server project with domain-driven architecture. Feature extends existing `partnership/` domain module following established patterns (repository pattern, route-layer orchestration, shared test database).
