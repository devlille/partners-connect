# Feature Specification: Company Test Codebase Refactoring

**Feature Branch**: `009-refactor-company-tests`  
**Created**: 11 November 2025  
**Status**: Draft  
**Input**: User description: "I would like to refactor my testing codebase to apply my guideline to all my package but to start, I want to scope this spec on the company test codebase (server/application/src/test/kotlin/fr/devlille/partners/connect/companies). Inside, I want to create contract codebase in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api and integration tests in server/application/src/test/kotlin/fr/devlille/partners/connect/companies. To do that, we'll need to rewrite existing test cases to create contract and integrations tests but I want to conserve acceptance tests used in my implementation."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Contract Test Reorganization (Priority: P1)

As a developer, I want contract tests to be clearly separated from integration tests so that I can understand the API schema validation independently from business logic testing.

**Why this priority**: This establishes the foundation for the testing architecture and provides immediate clarity about API contracts vs business logic.

**Independent Test**: Can be fully tested by running contract tests in isolation and verifying they only validate request/response schemas without business logic side effects.

**Acceptance Scenarios**:

1. **Given** existing API tests, **When** contract tests are extracted to dedicated directory structure, **Then** they focus solely on API schema validation
2. **Given** contract tests are created, **When** they are executed, **Then** they validate request/response formats without testing business logic
3. **Given** contract tests exist, **When** a developer reviews them, **Then** the tests clearly show API contract requirements

---

### User Story 2 - Integration Test Restructuring (Priority: P2)

As a developer, I want integration tests to be organized in the domain root directory so that I can understand end-to-end business workflows separately from API contracts.

**Why this priority**: This separates business logic validation from schema validation, making tests easier to understand and maintain.

**Independent Test**: Can be fully tested by running integration tests and verifying they test complete business scenarios from HTTP request to database persistence.

**Acceptance Scenarios**:

1. **Given** existing tests, **When** integration tests are moved to domain root directory, **Then** they test complete business workflows
2. **Given** integration tests are restructured, **When** they are executed, **Then** they validate business logic end-to-end including data persistence interactions
3. **Given** integration tests exist, **When** a developer reviews them, **Then** the tests clearly show business scenario coverage

---

### User Story 3 - Acceptance Test Preservation (Priority: P3)

As a developer, I want existing acceptance tests to be preserved during refactoring so that the current test coverage and validation scenarios remain intact.

**Why this priority**: This ensures no regression in test coverage while improving test organization.

**Independent Test**: Can be fully tested by comparing test coverage reports before and after refactoring to ensure equivalent coverage is maintained.

**Acceptance Scenarios**:

1. **Given** existing acceptance tests, **When** refactoring occurs, **Then** all existing test scenarios are preserved in either contract or integration tests
2. **Given** refactored test structure, **When** tests are executed, **Then** the same business scenarios are validated as before refactoring
3. **Given** preserved acceptance tests, **When** a developer reviews them, **Then** they can identify which scenarios are covered in the new structure

---

### Edge Cases

- What happens when existing tests cover both contract and integration concerns in a single test file? → Tests will be duplicated and split into focused variants for each category
- How does the system handle test files that don't clearly fit into either contract or integration categories? → Default to integration test category for safety
- What happens when test data utilities need to be shared between contract and integration tests? → Entity factories will be located in shared factories folder for H2 database initialization

## Requirements *(mandatory)*

### Functional Requirements

### Functional Requirements

- **FR-001**: System MUST reorganize existing test files into contract tests and integration tests using separate directory structures
- **FR-002**: Contract tests MUST focus exclusively on API request/response validation using existing test data patterns, without modifying database state beyond minimal test setup
- **FR-003**: Integration tests MUST validate complete business workflows from API endpoints to data persistence, including database state changes and business rule enforcement
- **FR-004**: System MUST preserve all existing test scenarios during reorganization by duplicating tests with mixed concerns and splitting them into focused contract and integration variants, with ambiguous tests defaulting to integration category for safety
- **FR-005**: Refactored tests MUST maintain equivalent test coverage levels as the original test suite
- **FR-006**: Contract tests MUST validate API schemas using JSON schema files and `call.receive<T>(schema)` pattern for all endpoints accepting request bodies, following constitutional requirements
- **FR-007**: Integration tests MUST use isolated test environments with transactional cleanup for test isolation
- **FR-008**: Both test types MUST follow established naming conventions for test files and test data functions
- **FR-009**: System MUST maintain existing entity factory functions for test data creation in a shared factories folder, accessible to both contract and integration tests for H2 in-memory database initialization

### Key Entities *(include if feature involves data)*

### Key Entities *(include if feature involves data)*

- **Contract Tests**: Test files that validate API request/response schemas, parameter validation, and data serialization without business logic
- **Integration Tests**: Test files that validate end-to-end business workflows including data persistence and business rule enforcement
- **Test Data Utilities**: Entity factory functions located in shared factories folder for creating test data and initializing H2 in-memory database, accessible to both test categories
- **Test Coverage Metrics**: Measurements showing percentage of code covered by tests before and after refactoring

## Clarifications

### Session 2025-11-11

- Q: Test categorization decision rules for mixed-concern tests → A: Duplicate mixed tests and split into focused contract and integration variants
- Q: Performance execution timeframes for measurable validation → A: both, contract and integration tests should be executed in less than 2 seconds
- Q: Ambiguous test categorization handling → A: Default to integration test category for safety
- Q: Test data utility sharing strategy → A: Initial test data should be entity factories usage to insert initial data in the H2 in-memory database, nothing else but they should be shared between contract and integration tests and located in the factories folder
- Q: Refactoring progress tracking approach → A: Keep nothing for now, I'll work on test coverage later

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Contract tests execute in under 2 seconds and validate only API schemas without persistent data interactions
- **SC-002**: Integration tests maintain 95%+ coverage of existing business logic scenarios
- **SC-003**: Test suite maintains equivalent or better execution performance compared to original test structure
- **SC-004**: 100% of existing test scenarios are preserved in either contract or integration test categories
- **SC-005**: Contract tests are clearly identifiable by their directory structure and naming conventions
- **SC-006**: Integration tests execute in under 2 seconds and validate complete business workflows

## Assumptions

- Existing test suite has good coverage of business scenarios that should be preserved
- Current test data creation patterns are reusable across both test categories
- Test execution environment supports isolated test runs with proper cleanup
- Development team values clear separation of API contract validation from business logic testing
