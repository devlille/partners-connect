# Quickstart: Company Test Codebase Refactoring

## Quick Validation Steps

### Prerequisites
- Kotlin/JVM 21 development environment
- Access to existing company test codebase
- H2 in-memory database test infrastructure

### Step 1: Analyze Current Test Structure

**Objective**: Understand existing test organization and identify refactoring candidates

```bash
# Navigate to company test directory
cd server/application/src/test/kotlin/fr/devlille/partners/connect/companies

# List current test files
ls -la *.kt

# Review existing factory structure  
ls -la factories/
```

**Expected Results**:
- See existing test files (~15 files)
- Identify mixed-concern tests vs focused tests
- Confirm factories directory exists

### Step 2: Create New Directory Structure

**Objective**: Establish contract and integration test organization

```bash
# Create contract test directory
mkdir -p infrastructure/api

# Verify existing factories directory
ls factories/
```

**Expected Results**:
- `infrastructure/api/` directory created for contract tests
- `factories/` directory preserved for shared utilities
- Domain root directory available for integration tests

### Step 3: Run Existing Tests (Baseline)

**Objective**: Establish current test coverage and performance baseline

```bash
# Navigate to server directory
cd server/

# Run company module tests
./gradlew test --tests "*companies*" --no-daemon

# Note execution time and coverage
```

**Expected Results**:
- All existing tests pass
- Note total execution time (baseline for <2 second requirement)
- Confirm H2 database and factories work correctly

### Step 4: Create First Contract Test

**Objective**: Validate contract test approach with simple API schema validation

Create `infrastructure/api/CompanyCreateContractTest.kt`:

```kotlin
package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyCreateContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST companies should accept valid CreateCompany request and return 201 with ID`() = testApplication {
        application { moduleMocked() }

        val createRequest = CreateCompany(
            name = "Test Company",
            siteUrl = "https://test.com",
            headOffice = Address(
                address = "123 Test St",
                city = "Test City", 
                zipCode = "12345",
                country = "FR"
            ),
            siret = "12345678901234",
            vat = "FR12345678901",
            description = "Test company description",
            socials = emptyList()
        )

        val response = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(createRequest)
        }

        // Contract validation: HTTP status and response schema
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody.containsKey("id"))
        
        // Validate ID is valid UUID format
        val companyId = responseBody["id"]?.toString()?.trim('"')
        UUID.fromString(companyId) // Should not throw exception
    }

    @Test
    fun `POST companies should return 400 for missing required fields`() = testApplication {
        application { moduleMocked() }

        val invalidRequest = mapOf("name" to "") // Missing required fields

        val response = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(invalidRequest)
        }

        // Contract validation: Error status code
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().isNotBlank())
    }
}
```

**Run the test**:
```bash
# Run the new contract test
./gradlew test --tests "*CompanyCreateContractTest*" --no-daemon
```

**Expected Results**:
- Test passes (validates existing API)
- Execution time <2 seconds
- Clear separation of schema validation concerns

### Step 5: Create First Integration Test

**Objective**: Validate integration test approach with business logic validation

Create `CompanyLifecycleIntegrationTest.kt`:

```kotlin
package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.UpdateCompany
import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyLifecycleIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `should create and update company with complete business validation`() = testApplication {
        application { moduleMocked() }

        // Create company
        val createRequest = CreateCompany(
            name = "Integration Test Company",
            siteUrl = "https://integration-test.com",
            headOffice = Address(
                address = "456 Integration St",
                city = "Integration City",
                zipCode = "54321", 
                country = "FR"
            ),
            siret = "98765432109876",
            vat = "FR98765432109",
            description = "Integration test company",
            socials = emptyList()
        )

        val createResponse = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(createRequest)
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val companyId = json.parseToJsonElement(createResponse.bodyAsText())
            .jsonObject["id"]?.toString()?.trim('"')

        // Verify company was persisted correctly
        val getResponse = client.get("/companies/$companyId")
        assertEquals(HttpStatusCode.OK, getResponse.status)

        // Update company - business logic validation
        val updateRequest = UpdateCompany(
            name = "Updated Integration Company",
            siteUrl = "https://updated-integration.com"
        )

        val updateResponse = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify update was applied correctly
        val verifyResponse = client.get("/companies/$companyId")
        val companyData = json.parseToJsonElement(verifyResponse.bodyAsText()).jsonObject
        assertEquals("\"Updated Integration Company\"", companyData["name"].toString())
    }
}
```

**Run the test**:
```bash
# Run the new integration test
./gradlew test --tests "*CompanyLifecycleIntegrationTest*" --no-daemon
```

**Expected Results**:
- Test passes (validates business workflow)
- Execution time <2 seconds
- Clear validation of end-to-end functionality

### Step 6: Validate Factory Sharing

**Objective**: Confirm shared factories work for both test types

```bash
# Run both test types together
./gradlew test --tests "*Company*Contract*,*Company*Integration*" --no-daemon

# Verify factories are accessible
grep -r "insertMockedCompany" infrastructure/api/
grep -r "insertMockedCompany" CompanyLifecycleIntegrationTest.kt
```

**Expected Results**:
- Both contract and integration tests use shared factories
- No duplication of test data creation logic
- Consistent factory usage patterns

### Step 7: Performance Validation

**Objective**: Ensure refactored tests meet performance requirements

```bash
# Time contract test execution
time ./gradlew test --tests "*Contract*" --no-daemon

# Time integration test execution  
time ./gradlew test --tests "*Integration*" --no-daemon
```

**Expected Results**:
- Contract tests complete in <2 seconds
- Integration tests complete in <2 seconds
- Total test suite maintains or improves performance

### Step 8: Coverage Preservation Check

**Objective**: Validate that refactoring preserves test scenarios

```bash
# Run full company test suite
./gradlew test --tests "*companies*" --no-daemon

# Compare against baseline from Step 3
```

**Expected Results**:
- All original test scenarios covered in new structure
- No test failures from refactoring
- Coverage maintained or improved

## Validation Checklist

After completing all steps, verify:

- [ ] Contract tests exist in `infrastructure/api/` directory
- [ ] Integration tests exist in domain root directory
- [ ] Shared factories accessible to both test types
- [ ] All tests execute in <2 seconds per category
- [ ] Original test scenarios preserved
- [ ] New test structure follows naming conventions
- [ ] Tests pass ktlint/detekt quality checks

## Next Steps

1. **Full Refactoring**: Apply same pattern to remaining test files
2. **Documentation**: Update test documentation to reflect new structure  
3. **Team Training**: Share refactoring patterns with development team
4. **Continuous Validation**: Run tests regularly during refactoring process

## Troubleshooting

**Common Issues**:

- **Import errors**: Ensure factories package is properly accessible
- **Performance issues**: Review H2 database setup and transaction handling
- **Test failures**: Check for missing test data setup or business logic changes

**Resolution Steps**:
1. Verify directory structure matches specification
2. Check factory function imports and usage
3. Validate H2 database configuration
4. Review test execution output for specific error details