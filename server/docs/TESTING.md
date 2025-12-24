# Server Testing Guide

This guide defines the rules and patterns for writing tests in the `partners-connect` server application.

## Table of Contents
- [Test Architecture Overview](#test-architecture-overview)
- [Shared Database Configuration](#shared-database-configuration)
- [Factory Functions](#factory-functions)
- [Contract Tests (Unit Tests)](#contract-tests-unit-tests)
- [Integration Tests](#integration-tests)
- [Best Practices](#best-practices)
- [Migration from Old Pattern](#migration-from-old-pattern)

## Test Architecture Overview

The test architecture has been refactored to use a **shared in-memory H2 database** across all test cases instead of isolated databases per test. This approach provides better performance and more realistic test scenarios while maintaining test isolation through careful data management.

### Key Principles

1. **Shared Database**: All tests share a single in-memory H2 database (`jdbc:h2:mem:test;DB_CLOSE_DELAY=-1`)
2. **Pre-created UUIDs**: Use predefined UUIDs before initializing data to avoid conflicts
3. **Unique Identifiers**: Use much more specific and unique identifiers in factory functions to prevent database constraint violations
4. **Single Transaction per Test**: Initialize all test data in a single transaction block
5. **Granular Factories**: Each factory function does one specific thing
6. **No Transactions in Factories**: Factory functions must not manage transactions themselves

## Shared Database Configuration

### Using `moduleSharedDb`

The `ApplicationMock.moduleSharedDb()` function configures a shared database for all test cases. **This is now the default** and should be used instead of `moduleMocked()` unless you have a specific reason.

```kotlin
import fr.devlille.partners.connect.internal.moduleSharedDb

@Test
fun `test name`() = testApplication {
    val userId = UUID.randomUUID()
    
    application {
        moduleSharedDb(userId = userId)
        transaction {
            // Initialize all test data here in a single transaction
            insertMockedOrganisationEntity(orgId)
            insertMockedFutureEvent(eventId, orgId = orgId)
            insertMockedCompany(companyId)
        }
    }
    
    // Execute test assertions
}
```

### When to Use `moduleMocked()`

Only use `moduleMocked()` when you need:
- A completely isolated database per test (rare cases)
- Custom mock configurations that differ from the default shared setup

## Factory Functions

Factory functions create test data and follow a consistent naming convention and structure.

### Naming Convention

- **Entity Factories**: `insertMocked<EntityName>` - Creates and persists entities in the database
  - Example: `insertMockedCompany()`, `insertMockedPartnership()`, `insertMockedEvent()`
  
- **Domain Object Factories**: `create<DomainName>` - Creates domain objects without database persistence
  - Example: `createOrganisation()`, `createEvent()`

### Factory File Naming

- Factory files must use the `.factory.kt` extension
- Examples:
  - `Company.factory.kt`
  - `Partnership.factory.kt`
  - `EventEntity.factory.kt`
  - `Organisation.factory.kt`

### Factory Function Rules

1. **Single Responsibility**: Each factory does one specific thing
2. **No Transaction Management**: Factories must NOT wrap operations in transactions
3. **Pre-created UUIDs**: Accept UUID parameters (with random defaults)
4. **Unique Default Values**: Use UUID-based names or unique identifiers as defaults
5. **All Parameters Optional**: Provide sensible defaults for all parameters

### Example Factory Function

```kotlin
package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(), // Unique default using UUID
    address: String = "123 Mock St",
    city: String = "Mock City",
    zipCode: String = "12345",
    country: String = "MO",
    siret: String = "12345678901234",
    vat: String = "FR12345678901",
    description: String? = "This is a mock company for testing purposes.",
    siteUrl: String = "https://www.mockcompany.com",
    status: CompanyStatus = CompanyStatus.ACTIVE,
): CompanyEntity = CompanyEntity.new(id) {
    this.name = name
    this.address = address
    this.city = city
    this.zipCode = zipCode
    this.country = country
    this.siret = siret
    this.vat = vat
    this.description = description
    this.siteUrl = siteUrl
    this.logoUrlOriginal = null
    this.logoUrl1000 = null
    this.logoUrl500 = null
    this.logoUrl250 = null
    this.status = status
}
```

### Using Factories in Tests

Always initialize data in a **single transaction block** at the start of your test:

```kotlin
@Test
fun `test scenario`() = testApplication {
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    val companyId = UUID.randomUUID()
    val partnershipId = UUID.randomUUID()

    application {
        moduleSharedDb(userId)
        transaction {
            // All data initialization in one transaction
            insertMockedUser(userId)
            insertMockedOrganisationEntity(orgId)
            insertMockedOrgaPermission(orgId, userId = userId)
            insertMockedFutureEvent(eventId, orgId = orgId)
            insertMockedCompany(companyId)
            insertMockedPartnership(partnershipId, eventId, companyId)
        }
    }
    
    // Test execution follows
}
```

## Contract Tests (Unit Tests)

Contract tests validate the **HTTP contract** of individual endpoints, ensuring they return the correct status codes and response structures for all supported scenarios.

### Contract Test Characteristics

- **Location**: `<feature>.infrastructure.api` package
- **Scope**: Single endpoint, multiple HTTP status codes
- **Naming**: `<Feature><EndpointResource>Route<Verb>Test`
- **Purpose**: Validate request/response schemas and HTTP semantics
- **Focus**: API contract, NOT business logic

### Naming Convention

Pattern: `<Feature><EndpointResource>Route<Verb>Test`

Examples:
- `PartnershipRegisterRoutePostTest` - Tests POST /events/{eventId}/partnerships
- `CompanyJobOfferRouteDeleteTest` - Tests DELETE /companies/{companyId}/job-offers/{id}
- `EventBySlugRouteGetTest` - Tests GET /events/{slug}
- `SponsoringPackCreationRoutePostTest` - Tests POST /events/{eventId}/sponsoring/packs

### Contract Test Structure

```kotlin
package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipRegisterRoutePostTest {
    
    @Test
    fun `POST registers a valid partnership - returns 201 Created`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
            }
        }

        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody("""{"packId":"...", "companyId":"$companyId"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }
    
    @Test
    fun `POST with invalid company ID - returns 404 Not Found`() = testApplication {
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedFutureEvent(eventId)
            }
        }

        val invalidCompanyId = UUID.randomUUID()
        val response = client.post("/events/$eventId/partnerships") {
            contentType(ContentType.Application.Json)
            setBody("""{"companyId":"$invalidCompanyId"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
    
    @Test
    fun `POST with missing required field - returns 400 Bad Request`() = testApplication {
        val userId = UUID.randomUUID()

        application { moduleSharedDb(userId) }

        val response = client.post("/events/${UUID.randomUUID()}/partnerships") {
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
```

### Coverage Requirements for Contract Tests

A complete contract test must verify:

✅ **Success case** (200/201/204)
✅ **Not Found** (404) - when resource doesn't exist
✅ **Bad Request** (400) - when input validation fails
✅ **Unauthorized** (401) - when authentication is missing
✅ **Forbidden** (403) - when user lacks permissions
✅ **Conflict** (409) - when business rules prevent the operation
✅ Any other status codes specific to the endpoint

## Integration Tests

Integration tests validate **end-to-end workflows** across multiple endpoints within a feature domain, focusing on business logic and cross-cutting concerns.

### Integration Test Characteristics

- **Location**: `<feature>` package (root of the domain)
- **Scope**: Multiple endpoints, complete workflows
- **Naming**: `<Feature>(<EndpointResource>)RoutesTest` (note the plural "Routes")
- **Purpose**: Validate business logic, workflows, and cross-domain interactions
- **Focus**: Realistic scenarios with minimal setup

### Naming Convention

Pattern: `<Feature>(<EndpointResource>)RoutesTest`

Examples:
- `PartnershipSpeakersRoutesTest` - Tests speaker attachment/detachment workflow
- `CompanyJobOfferRoutesTest` - Tests complete job offer lifecycle
- `ProvidersAttachmentRoutesTest` - Tests provider attachment workflow
- `PartnershipDeletionRoutesTest` - Tests partnership deletion scenarios

### Integration Test Structure

```kotlin
package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for complete speaker-partnership workflow scenarios.
 * Tests end-to-end business logic from agenda import to speaker attachment.
 */
class PartnershipSpeakersRoutesTest {
    
    @Test
    fun `complete import and attachment workflow works end-to-end`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(partnershipId, eventId, companyId, validatedAt = now())
                insertMockedSpeaker(speakerId, eventId = eventId)
            }
        }

        // Step 1: Attach speaker to partnership
        val attachResponse = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.Created, attachResponse.status)

        // Step 2: Verify partnership includes speaker
        val partnershipResponse = client.get("/events/$eventId/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, partnershipResponse.status)
        // Assert response contains speaker

        // Step 3: Detach speaker
        val detachResponse = client.delete("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.NoContent, detachResponse.status)

        // Step 4: Verify speaker removed
        val finalResponse = client.get("/events/$eventId/partnerships/$partnershipId")
        // Assert speaker no longer in response
    }
}
```

### Integration Test Requirements

✅ **Minimal Setup**: Only create data necessary for the workflow
✅ **Complete Scenarios**: Test realistic end-to-end user journeys
✅ **Cross-Domain Logic**: Validate interactions between multiple features
✅ **Business Rules**: Verify complex business logic and state transitions
✅ **Clear Steps**: Document the workflow with comments or step markers

## Best Practices

### 1. Data Isolation and Uniqueness

Since all tests share the same database, ensure data uniqueness:

```kotlin
// ✅ GOOD: Pre-create UUIDs
val companyId = UUID.randomUUID()
val eventId = UUID.randomUUID()
insertMockedCompany(id = companyId, name = companyId.toString())

// ❌ BAD: Hardcoded values that might conflict
insertMockedCompany(name = "Test Company")
```

### 2. Single Transaction for Setup

Group all data initialization in one transaction:

```kotlin
// ✅ GOOD: Single transaction
transaction {
    insertMockedUser(userId)
    insertMockedOrganisationEntity(orgId)
    insertMockedEvent(eventId, orgId)
}

// ❌ BAD: Multiple transactions
transaction { insertMockedUser(userId) }
transaction { insertMockedOrganisationEntity(orgId) }
transaction { insertMockedEvent(eventId, orgId) }
```

### 3. Careful with Constraints

Be mindful of database constraints (unique indexes, foreign keys):

```kotlin
// Use unique identifiers for fields with unique constraints
insertMockedCompany(
    id = companyId,
    name = companyId.toString(), // Ensures uniqueness
    siret = "SIRET-${companyId}", // Unique SIRET
    vat = "VAT-${companyId}" // Unique VAT
)
```

### 4. Factory Granularity

Create granular factory functions for complex setups:

```kotlin
// ✅ GOOD: Separate concerns
fun insertMockedSponsoringPack(id: UUID, eventId: UUID): SponsoringPackEntity
fun insertMockedSponsoringOption(id: UUID, eventId: UUID): SponsoringOptionEntity
fun insertMockedPackOptions(packId: UUID, optionId: UUID): PackOptionsEntity

// ❌ BAD: Monolithic factory
fun insertMockedCompleteSponsoring(/* too many parameters */)
```

### 5. Test Organization

```
server/application/src/test/kotlin/fr/devlille/partners/connect/
├── <feature>/
│   ├── <Feature>RoutesTest.kt           # Integration tests
│   ├── factories/
│   │   ├── Entity.factory.kt            # Entity factories
│   │   └── DomainObject.factory.kt      # Domain factories
│   └── infrastructure/
│       └── api/
│           ├── <Feature>RouteGetTest.kt # Contract tests
│           ├── <Feature>RoutePostTest.kt
│           └── <Feature>RouteDeleteTest.kt
```

## Migration from Old Pattern

### What Changed

| Old Approach | New Approach |
|--------------|--------------|
| `moduleMocked()` with unique DB per test | `moduleSharedDb()` with shared database |
| Transactions in factory functions | No transactions in factories |
| Random/generic identifiers | UUID-based unique identifiers |
| Monolithic factory functions | Granular, single-purpose factories |
| Mixed contract and integration tests | Clear separation by package |
| Generic test names | Structured naming convention |

### Migration Checklist

When updating existing tests:

- [ ] Replace `moduleMocked()` with `moduleSharedDb()`
- [ ] Remove transactions from factory functions
- [ ] Add pre-created UUIDs for all entities
- [ ] Use UUID-based default values in factories
- [ ] Wrap all setup in a single `transaction {}` block
- [ ] Move contract tests to `infrastructure.api` package
- [ ] Move integration tests to feature root package
- [ ] Rename tests following conventions
- [ ] Split monolithic tests into focused contract and integration tests

## Running Tests

```bash
# Run all tests
cd server
./gradlew test --no-daemon

# Run tests for a specific feature
./gradlew test --tests "fr.devlille.partners.connect.partnership.*" --no-daemon

# Run a specific test class
./gradlew test --tests "PartnershipRegisterRoutePostTest" --no-daemon
```

## Troubleshooting

### Database Constraint Violations

**Symptom**: `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException`

**Solution**: Use unique identifiers in factory defaults:
```kotlin
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(), // Unique!
)
```

### Test Data Bleeding Between Tests

**Symptom**: Tests pass individually but fail when run together

**Solution**: Always use pre-created UUIDs and unique identifiers:
```kotlin
val companyId = UUID.randomUUID()
val eventId = UUID.randomUUID()
```

### Transaction Already Active

**Symptom**: `java.lang.IllegalStateException: Transaction is already active`

**Solution**: Remove transaction management from factory functions - only the test should manage transactions.

## Summary

The refactored test architecture provides:

✅ **Better Performance**: Shared database reduces initialization overhead
✅ **Clearer Structure**: Contract vs. Integration test separation
✅ **Easier Maintenance**: Consistent naming and organization
✅ **More Realistic**: Tests run against the same database state
✅ **Better Isolation**: Pre-created UUIDs prevent conflicts

Follow these guidelines when contributing tests to ensure consistency and maintainability across the codebase.
