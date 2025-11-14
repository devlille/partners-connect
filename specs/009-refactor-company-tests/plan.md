# Implementation Plan: Company Test Codebase Refactoring

**Branch**: `009-refactor-company-tests` | **Date**: 11 November 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-refactor-company-tests/spec.md`

## Summary

Reorganize existing company test codebase into clearly separated contract tests and integration tests. Contract tests will focus on API schema validation in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api` while integration tests will validate complete business workflows in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies`. Tests with mixed concerns will be duplicated and split into focused variants, preserving all existing test scenarios while improving test organization and maintainability.

## Technical Context

**Language/Version**: Kotlin (JVM 21)  
**Primary Dependencies**: Ktor, Exposed ORM, JUnit, Mockk, H2 in-memory database  
**Storage**: H2 in-memory database for test isolation with transaction rollback  
**Testing**: JUnit 5, Ktor testing framework, existing mock factory patterns  
**Target Platform**: JVM server environment (existing test infrastructure)  
**Project Type**: Server-side test refactoring (existing Kotlin/Ktor backend)  
**Performance Goals**: Contract tests <2 seconds, Integration tests <2 seconds execution time  
**Constraints**: Preserve 100% of existing test scenarios, maintain equivalent coverage  
**Scale/Scope**: Company module test files (~15 existing test files based on repository exploration)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: ✅ PASS - Refactoring will maintain ktlint/detekt compliance, improve test organization and readability following existing naming conventions and documentation standards.

**Comprehensive Testing Strategy**: ✅ PASS - Feature specifically implements constitutional requirement for separation of contract tests (API schema validation) from integration tests (business logic validation). Preserves existing HTTP route integration test coverage while organizing tests according to constitutional principles.

**Clean Modular Architecture**: ✅ PASS - Refactoring operates within existing companies domain module boundaries, maintains repository layer separation, and follows established directory structure patterns. No new dependencies or architectural changes.

**API Consistency & User Experience**: ✅ PASS - No API changes required. Refactoring improves test maintainability which supports continued API quality through better test organization. Performance requirements (2-second execution) align with constitutional standards.

**Performance & Observability Requirements**: ✅ PASS - Focus on test execution performance optimization. Feature does not require production performance testing per constitutional guidance. Test refactoring supports long-term maintainability and observability of test results.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
server/application/src/test/kotlin/fr/devlille/partners/connect/
└── companies/
    ├── infrastructure/
    │   └── api/                          # CONTRACT TESTS (NEW STRUCTURE)
    │       ├── CompanyRoutesContractTest.kt
    │       ├── CompanyJobOfferRoutesContractTest.kt
    │       ├── CompanyUpdateContractTest.kt
    │       ├── CompanyLogoUploadContractTest.kt
    │       └── [other contract test files...]
    ├── CompanyIntegrationTest.kt          # INTEGRATION TESTS (REFACTORED)
    ├── CompanyJobOfferIntegrationTest.kt
    ├── CompanyUpdateIntegrationTest.kt
    ├── CompanyDeleteIntegrationTest.kt
    ├── [other integration test files...]
    └── factories/                         # SHARED TEST DATA (PRESERVED)
        ├── CompanyFactories.kt
        ├── JobOfferFactories.kt
        └── [existing factory functions...]
```

**Structure Decision**: Server-side test refactoring within existing companies domain module. Contract tests organized in `infrastructure/api/` subdirectory to align with Clean Architecture principles, integration tests remain in domain root directory. Shared factories folder preserved for H2 database initialization utilities accessible to both test categories. Follows constitutional requirement for modular test organization while maintaining existing test infrastructure and patterns.

## Phase 0: Research & Analysis

*Prerequisites: Constitution Check complete*

**Research Completed**: ✅ [research.md](./research.md)
- Analyzed existing test structure and categorization strategy  
- Defined systematic rules for contract vs integration test separation
- Established directory organization and naming conventions
- Validated performance optimization approach with H2 database
- Confirmed constitutional compliance for both test categories

## Phase 1: Design & Contracts

*Prerequisites: research.md complete*

**Data Model**: ✅ [data-model.md](./data-model.md)
- Defined contract test, integration test, and shared factory entities
- Established test file organization and naming patterns
- Created refactoring mapping strategy for existing tests
- Designed test scenario preservation matrix

**Contracts**: ✅ [contracts/](./contracts/)
- Specified contract test files for API schema validation
- Defined integration test files for business logic validation  
- Established performance and coverage requirements
- Created quality contracts for code compliance

**Quickstart Guide**: ✅ [quickstart.md](./quickstart.md)
- Step-by-step validation process for refactoring approach
- Example contract and integration test implementations
- Performance validation procedures
- Troubleshooting guide for common issues

**Agent Context**: ✅ Updated via `.specify/scripts/bash/update-agent-context.sh copilot`
- Added Kotlin/JVM 21 and testing framework details to copilot context

## Constitution Check (Post-Design)

*GATE: Re-evaluation after Phase 1 design completion*

**Code Quality Standards**: ✅ PASS - Design maintains ktlint/detekt compliance, follows established naming conventions, and improves test organization per constitutional requirements.

**Comprehensive Testing Strategy**: ✅ PASS - Design explicitly implements constitutional separation of contract tests (API schema validation) and integration tests (business logic validation). Maintains 80%+ coverage requirement and H2 in-memory database testing approach.

**Clean Modular Architecture**: ✅ PASS - Refactoring maintains existing companies domain module structure, follows Clean Architecture with contract tests in infrastructure/api layer, preserves factory pattern for shared utilities.

**API Consistency & User Experience**: ✅ PASS - No API changes required. Refactoring improves test maintainability supporting continued API quality. Performance requirements align with constitutional 2-second standards.

**Performance & Observability Requirements**: ✅ PASS - Design addresses test execution performance optimization while maintaining constitutional focus on functional validation over performance testing in specifications.

## Phase 2: Planning Complete

**Status**: ✅ READY FOR IMPLEMENTATION

**Branch**: `009-refactor-company-tests`  
**Implementation Plan**: `/Users/mac-GPALIG05/Documents/workspace/partners-connect/specs/009-refactor-company-tests/plan.md`

**Generated Artifacts**:
- [research.md](./research.md) - Analysis and decision rationale
- [data-model.md](./data-model.md) - Test entity organization and refactoring mapping
- [contracts/test-contracts.md](./contracts/test-contracts.md) - Contract and integration test specifications  
- [quickstart.md](./quickstart.md) - Step-by-step validation guide

**Next Command**: `/speckit.tasks` - Generate detailed implementation tasks

**Key Implementation Requirements**:
- Contract tests in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/`
- Integration tests in `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/`  
- Preserve shared factories in existing factories folder
- Maintain <2 second execution time for both test categories
- Ensure 100% preservation of existing test scenarios
