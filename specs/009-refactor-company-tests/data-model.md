# Data Model: Company Test Codebase Refactoring

## Test Category Entities

### Contract Test
**Purpose**: Validate API request/response schemas and HTTP protocol compliance
**Scope**: Schema validation, serialization, status codes
**Lifecycle**: Fast execution (<2 seconds), minimal business setup

**Key Attributes**:
- Test method focuses on single API endpoint
- Minimal entity setup using factory functions
- Validates request/response structure
- Checks HTTP status codes
- No complex business logic validation

**Test Data Requirements**:
- Single company entity via `insertMockedCompany()`
- Minimal related entities (e.g., single job offer)
- No complex relationships or business state

### Integration Test
**Purpose**: Validate end-to-end business workflows and domain logic
**Scope**: Business rules, cross-domain interactions, persistence
**Lifecycle**: Complete workflow testing (<2 seconds), full business setup

**Key Attributes**:
- Test method validates complete business scenario
- Complex entity setup with relationships
- Validates business rule enforcement
- Tests persistence and data consistency
- Includes cross-cutting concerns (notifications, etc.)

**Test Data Requirements**:
- Multiple related entities via factory functions
- Complex business state setup
- Cross-domain relationships (company → events → partnerships)

### Shared Test Factory
**Purpose**: Provide reusable test data creation for both test categories
**Scope**: H2 database initialization, entity creation utilities
**Lifecycle**: Persistent across test categories, accessible to all tests

**Key Attributes**:
- Entity factory functions following `insertMocked*()` pattern
- H2 in-memory database compatible
- Reusable across contract and integration tests
- Maintains existing function signatures for compatibility

**Factory Functions**:
- `insertMockedCompany()` - Company entity creation
- `insertMockedJobOffer()` - Job offer entity creation
- `insertMockedPartnership()` - Partnership entity creation
- Additional factories as needed for test scenarios

## Test File Organization Structure

### Contract Test Package
**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/`
**Naming Pattern**: `[Domain][Action]ContractTest.kt`

**File Structure**:
- `CompanyCreateContractTest.kt` - POST /companies validation
- `CompanyGetContractTest.kt` - GET /companies/{id} validation  
- `CompanyListContractTest.kt` - GET /companies validation
- `CompanyUpdateContractTest.kt` - PUT /companies/{id} validation
- `CompanyLogoUploadContractTest.kt` - POST /companies/{id}/logo validation
- `CompanyJobOfferContractTest.kt` - Job offer endpoint contracts
- `CompanyPromotionContractTest.kt` - Promotion endpoint contracts

### Integration Test Package
**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/`
**Naming Pattern**: `[Domain][Workflow]IntegrationTest.kt`

**File Structure**:
- `CompanyLifecycleIntegrationTest.kt` - Create, update, delete workflows
- `CompanyJobOfferManagementIntegrationTest.kt` - Job offer business logic
- `CompanyPartnershipWorkflowIntegrationTest.kt` - Partnership interactions
- `CompanyStatusManagementIntegrationTest.kt` - Status transition logic
- `CompanySearchFilteringIntegrationTest.kt` - Search and filtering logic

### Shared Factory Package
**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/factories/`
**Preservation**: Maintain existing factory structure and functions

**Factory Organization**:
- Preserve existing `insertMocked*()` functions
- Ensure H2 database compatibility
- Maintain function signatures for backward compatibility
- Add new factory functions as needed for test scenarios

## Test Data Flow Relationships

### Contract Test Flow
1. **Minimal Setup**: Use factory functions for essential entities only
2. **API Call**: Execute HTTP request with test data
3. **Schema Validation**: Verify request/response structure and types
4. **Status Validation**: Check appropriate HTTP status codes
5. **Cleanup**: Transaction rollback via H2 database

### Integration Test Flow
1. **Complex Setup**: Use factory functions to create related entities and business state
2. **Business Operation**: Execute complete business workflow
3. **Business Logic Validation**: Verify business rules and state changes
4. **Persistence Validation**: Check database state and consistency
5. **Cross-cutting Validation**: Verify notifications, logging, etc.
6. **Cleanup**: Transaction rollback via H2 database

### Factory Function Dependencies
**Shared Access Pattern**:
- Contract tests import factory functions for minimal setup
- Integration tests import factory functions for complex setup
- Factories maintain single responsibility for entity creation
- No business logic in factory functions (pure data creation)

## Refactoring Mapping Strategy

### Current Test → New Test Category Mapping

**CompanyRoutesTest.kt** (Mixed concerns):
- Contract aspects → `CompanyCreateContractTest.kt`, `CompanyGetContractTest.kt`, `CompanyListContractTest.kt`
- Integration aspects → `CompanyLifecycleIntegrationTest.kt`

**CompanyJobOffer*Test.kt** (Mostly contract):
- Schema validation → `CompanyJobOfferContractTest.kt`
- Business logic → `CompanyJobOfferManagementIntegrationTest.kt`

**Company*IntegrationTest.kt** (Already integration):
- Preserve as integration tests with potential renaming for clarity
- Maintain existing business logic validation

**Mixed Concern Tests**:
- Duplicate test scenarios
- Extract contract concerns to contract tests
- Extract integration concerns to integration tests
- Ensure 100% scenario preservation per requirements

### Test Scenario Preservation Matrix

| Original Test | Contract Test Coverage | Integration Test Coverage |
|---------------|----------------------|--------------------------|
| Company creation API | HTTP 201 response, schema validation | End-to-end company lifecycle |
| Company update API | PUT request/response validation | Business rule enforcement |
| Job offer management | CRUD operation schemas | Job offer business workflows |
| Company deletion | DELETE status codes | Soft delete business logic |
| Search/filtering | Query parameter validation | Search business logic |

**Validation Criteria**:
- Every original test scenario appears in either contract or integration category
- No test scenario is lost during refactoring
- Coverage metrics maintained or improved
- Execution time requirements met for both categories