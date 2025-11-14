# Research: Company Test Codebase Refactoring

## Current Test Structure Analysis

### Existing Test Files Analysis
Based on repository exploration, the current company test structure includes:

**Current Test Files**:
- `CompanyRoutesTest.kt` - Mixed contract and integration concerns
- `CompanyJobOfferRoutesCreateTest.kt` - Mostly contract-focused
- `CompanyJobOfferRoutesDeleteTest.kt` - Mostly contract-focused  
- `CompanyJobOfferRoutesGetTest.kt` - Mostly contract-focused
- `CompanyJobOfferRoutesListTest.kt` - Mostly contract-focused
- `CompanyJobOfferRoutesUpdateTest.kt` - Mostly contract-focused
- `CompanyUpdateIntegrationTest.kt` - Integration-focused
- `CompanySoftDeleteIntegrationTest.kt` - Integration-focused
- `CompanyStatusFilterIntegrationTest.kt` - Integration-focused
- `ListJobOfferPromotionsRouteTest.kt` - Mixed concerns
- `PromoteJobOfferRouteTest.kt` - Mixed concerns

**Current Test Factories**:
- `factories/` directory with existing mock factory functions
- Follows `insertMocked*()` naming pattern

## Test Categorization Strategy

### Decision: Automated Categorization Rules
**Rationale**: Based on constitutional testing requirements and clarification session, implement systematic rules for separating contract from integration tests.

**Contract Test Criteria**:
- Tests that validate HTTP request/response schemas
- Tests that check serialization/deserialization
- Tests that validate HTTP status codes without complex business logic
- Tests that use minimal business setup (single entity creation)

**Integration Test Criteria**:
- Tests that validate end-to-end business workflows
- Tests that involve multiple domain interactions
- Tests that test complex business rules and state transitions
- Tests that validate cross-cutting concerns (notifications, etc.)

**Mixed Test Handling**: Duplicate and split into focused variants per clarification decision.

### Decision: Directory Structure Pattern
**Rationale**: Follow Clean Architecture principles with contract tests in `infrastructure/api/` and integration tests in domain root.

**Contract Test Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/`
**Integration Test Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/`
**Shared Factories**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/factories/`

## Performance Optimization Strategy

### Decision: H2 In-Memory Database Optimization
**Rationale**: Both contract and integration tests must execute under 2 seconds per clarification requirements.

**Optimization Approaches**:
- Minimize database setup in contract tests (schema validation only)
- Use transaction rollback for test isolation (constitutional requirement)
- Reuse H2 database instance across tests where possible
- Optimize entity factory functions for minimal data creation

**Alternatives Considered**:
- Mock repositories for contract tests → Rejected because constitutional pattern uses H2 for all tests
- Separate test databases → Rejected because current infrastructure uses single H2 instance

## Test Data Sharing Strategy

### Decision: Centralized Factory Pattern
**Rationale**: Per clarification decision, entity factories should be shared between test categories in factories folder.

**Factory Organization**:
- Preserve existing `insertMocked*()` naming conventions
- Ensure factories work with H2 in-memory database initialization
- Make factories accessible to both contract and integration test packages
- Follow constitutional mock factory requirements

**Sharing Mechanism**:
- Keep factories in existing `factories/` subdirectory
- Import factories in both contract and integration test files
- Maintain existing factory function signatures for compatibility

## Refactoring Process Strategy

### Decision: Incremental File-by-File Approach
**Rationale**: Minimize risk and ensure test coverage preservation throughout refactoring process.

**Process Steps**:
1. Analyze each existing test file for contract vs integration concerns
2. Create new contract test files in `infrastructure/api/` directory
3. Create new integration test files in domain root directory  
4. For mixed-concern tests: duplicate and split into focused variants
5. Preserve all existing test scenarios in new structure
6. Run tests continuously to ensure no coverage loss

**Risk Mitigation**:
- Keep original test files until new structure is validated
- Run full test suite after each file refactoring
- Document mapping between old and new test coverage

## Naming Convention Strategy

### Decision: Descriptive Contract vs Integration Naming
**Rationale**: Clear naming helps developers understand test purpose and location.

**Contract Test Naming**: `[Domain][Action]ContractTest.kt` (e.g., `CompanyCreateContractTest.kt`)
**Integration Test Naming**: `[Domain][Workflow]IntegrationTest.kt` (e.g., `CompanyLifecycleIntegrationTest.kt`)

**Alternatives Considered**:
- Keep existing naming → Rejected because it doesn't clearly indicate test type
- Use suffixes like `*RoutesTest` → Rejected because integration tests aren't just about routes

## Constitutional Compliance Validation

### Decision: Maintain Existing Testing Patterns
**Rationale**: Refactoring should enhance, not replace, constitutional testing requirements.

**Contract Test Compliance**:
- Focus on API schema validation per constitutional requirements
- Use existing mock factory patterns
- Follow TDD approach where new tests are created
- Validate request/response serialization

**Integration Test Compliance**:
- Maintain HTTP route testing (not repository testing) per constitutional requirements
- Test end-to-end scenarios including database persistence
- Use H2 in-memory database with transaction rollback
- Cover business logic and cross-cutting concerns

**Quality Standards**:
- All refactored tests must pass ktlint/detekt checks
- Preserve existing test documentation and comments
- Maintain 2-second execution requirement for performance