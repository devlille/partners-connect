# Contract Tests: Complete CRUD Operations for Companies

## Test Overview
Integration tests for PUT /companies/{id} and DELETE /companies/{id} endpoints, plus enhanced GET /companies with status filtering. Tests use Ktor testing framework with H2 in-memory database.

## Test Categories

### 1. Update Company Tests

#### PUT /companies/{companyId}

**Successful Update Test**:
```kotlin
@Test
fun `should update company with valid partial data`() = testApplication {
    val companyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId, name = "Original Company")
    }
    
    val updateRequest = UpdateCompany(
        name = "Updated Company Name",
        description = "New description"
    )
    
    val response = client.put("/companies/$companyId") {
        contentType(ContentType.Application.Json)
        setBody(updateRequest)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val updated = response.body<Company>()
    updated.name shouldBe "Updated Company Name"
    updated.description shouldBe "New description"
    updated.status shouldBe CompanyStatus.ACTIVE
    // Other fields should remain unchanged
}
```

**Partial Update Test**:
```kotlin
@Test
fun `should handle partial updates without affecting other fields`() = testApplication {
    val companyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(
            companyId, 
            name = "Original Company",
            siret = "12345678901234",
            vat = "FR12345678901"
        )
    }
    
    val partialUpdate = UpdateCompany(name = "New Name Only")
    
    val response = client.put("/companies/$companyId") {
        contentType(ContentType.Application.Json)
        setBody(partialUpdate)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val updated = response.body<Company>()
    updated.name shouldBe "New Name Only"
    updated.siret shouldBe "12345678901234" // Unchanged
    updated.vat shouldBe "FR12345678901" // Unchanged
}
```

**Validation Error Test**:
```kotlin
@Test
fun `should return 400 for invalid update data`() = testApplication {
    val companyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId)
    }
    
    val invalidUpdate = UpdateCompany(
        siret = "invalid-siret", // Should be 14 digits
        vat = "invalid-vat" // Should match VAT format
    )
    
    val response = client.put("/companies/$companyId") {
        contentType(ContentType.Application.Json)
        setBody(invalidUpdate)
    }
    
    response.status shouldBe HttpStatusCode.BadRequest
    val error = response.body<ErrorResponse>()
    error.details["siret"] should contain("14 digits")
    error.details["vat"] should contain("Invalid VAT format")
}
```

**Company Not Found Test**:
```kotlin
@Test
fun `should return 404 when updating non-existent company`() = testApplication {
    val nonExistentCompanyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        // No company inserted
    }
    
    val updateRequest = UpdateCompany(name = "Updated Name")
    
    val response = client.put("/companies/$nonExistentCompanyId") {
        contentType(ContentType.Application.Json)
        setBody(updateRequest)
    }
    
    response.status shouldBe HttpStatusCode.NotFound
}
```

**Conflict Test (Duplicate SIRET)**:
```kotlin
@Test
fun `should return 409 when SIRET conflicts with another company`() = testApplication {
    val companyId1 = UUID.randomUUID()
    val companyId2 = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId1, siret = "11111111111111")
        insertMockedCompany(companyId2, siret = "22222222222222")
    }
    
    val conflictUpdate = UpdateCompany(siret = "11111111111111") // Conflicts with company1
    
    val response = client.put("/companies/$companyId2") {
        contentType(ContentType.Application.Json)
        setBody(conflictUpdate)
    }
    
    response.status shouldBe HttpStatusCode.Conflict
}
```

### 2. Soft Delete Company Tests

#### DELETE /companies/{companyId}

**Successful Soft Delete Test**:
```kotlin
@Test
fun `should soft delete company and return 204`() = testApplication {
    val companyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId, name = "Company To Delete")
    }
    
    val response = client.delete("/companies/$companyId")
    
    response.status shouldBe HttpStatusCode.NoContent
    
    // Verify company still exists but marked as inactive
    val getResponse = client.get("/companies/$companyId")
    getResponse.status shouldBe HttpStatusCode.OK
    val company = getResponse.body<Company>()
    company.status shouldBe CompanyStatus.INACTIVE
    company.name shouldBe "Company To Delete" // Data preserved
}
```

**Company Not Found Test**:
```kotlin
@Test
fun `should return 404 when deleting non-existent company`() = testApplication {
    val nonExistentCompanyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        // No company inserted
    }
    
    val response = client.delete("/companies/$nonExistentCompanyId")
    
    response.status shouldBe HttpStatusCode.NotFound
}
```

**Relationships Preserved Test**:
```kotlin
@Test
fun `should preserve company relationships after soft delete`() = testApplication {
    val companyId = UUID.randomUUID()
    val partnershipId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId)
        insertMockedPartnership(partnershipId, companyId = companyId)
    }
    
    // Soft delete company
    val deleteResponse = client.delete("/companies/$companyId")
    deleteResponse.status shouldBe HttpStatusCode.NoContent
    
    // Verify partnership still exists and references the company
    val partnershipResponse = client.get("/partnerships/$partnershipId")
    partnershipResponse.status shouldBe HttpStatusCode.OK
    val partnership = partnershipResponse.body<Partnership>()
    partnership.companyId shouldBe companyId.toString()
}
```

### 3. Enhanced Listing with Status Filter Tests

#### GET /companies with status parameter

**Default Behavior Test (All Companies)**:
```kotlin
@Test
fun `should return all companies by default`() = testApplication {
    val activeCompanyId = UUID.randomUUID()
    val inactiveCompanyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(activeCompanyId, name = "Active Company")
        insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
    }
    
    val response = client.get("/companies")
    
    response.status shouldBe HttpStatusCode.OK
    val result = response.body<PaginatedResponse<Company>>()
    result.total shouldBe 2
    result.items.size shouldBe 2
    
    val activeCompany = result.items.find { it.status == CompanyStatus.ACTIVE }
    val inactiveCompany = result.items.find { it.status == CompanyStatus.INACTIVE }
    
    activeCompany shouldNotBe null
    inactiveCompany shouldNotBe null
    activeCompany!!.name shouldBe "Active Company"
    inactiveCompany!!.name shouldBe "Inactive Company"
}
```

**Active Only Filter Test**:
```kotlin
@Test
fun `should return only active companies when status=active`() = testApplication {
    val activeCompanyId = UUID.randomUUID()
    val inactiveCompanyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(activeCompanyId, name = "Active Company")
        insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
    }
    
    val response = client.get("/companies?status=active")
    
    response.status shouldBe HttpStatusCode.OK
    val result = response.body<PaginatedResponse<Company>>()
    result.total shouldBe 1
    result.items.size shouldBe 1
    result.items[0].status shouldBe CompanyStatus.ACTIVE
    result.items[0].name shouldBe "Active Company"
}
```

**Inactive Only Filter Test**:
```kotlin
@Test
fun `should return only inactive companies when status=inactive`() = testApplication {
    val activeCompanyId = UUID.randomUUID()
    val inactiveCompanyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(activeCompanyId, name = "Active Company")
        insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
    }
    
    val response = client.get("/companies?status=inactive")
    
    response.status shouldBe HttpStatusCode.OK
    val result = response.body<PaginatedResponse<Company>>()
    result.total shouldBe 1
    result.items.size shouldBe 1
    result.items[0].status shouldBe CompanyStatus.INACTIVE
    result.items[0].name shouldBe "Inactive Company"
}
```

**Combined Search and Status Filter Test**:
```kotlin
@Test
fun `should combine query search with status filtering`() = testApplication {
    application {
        moduleMocked()
        insertMockedCompany(UUID.randomUUID(), name = "Tech Active Company")
        insertMockedCompany(UUID.randomUUID(), name = "Tech Inactive Company", status = CompanyStatus.INACTIVE)
        insertMockedCompany(UUID.randomUUID(), name = "Other Active Company")
    }
    
    val response = client.get("/companies?query=tech&status=active")
    
    response.status shouldBe HttpStatusCode.OK
    val result = response.body<PaginatedResponse<Company>>()
    result.total shouldBe 1
    result.items.size shouldBe 1
    result.items[0].name shouldBe "Tech Active Company"
    result.items[0].status shouldBe CompanyStatus.ACTIVE
}
```

**Invalid Status Parameter Test**:
```kotlin
@Test
fun `should return 400 for invalid status parameter`() = testApplication {
    application {
        moduleMocked()
    }
    
    val response = client.get("/companies?status=invalid_status")
    
    response.status shouldBe HttpStatusCode.BadRequest
}
```

### 4. Backwards Compatibility Tests

**Existing GET Company Test**:
```kotlin
@Test
fun `should include status field in individual company response`() = testApplication {
    val companyId = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedCompany(companyId, name = "Test Company")
    }
    
    val response = client.get("/companies/$companyId")
    
    response.status shouldBe HttpStatusCode.OK
    val company = response.body<Company>()
    company.status shouldBe CompanyStatus.ACTIVE
    company.name shouldBe "Test Company"
}
```

## Test Fixtures

### Mock Company Factory (Enhanced)
```kotlin
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = "Mock Company",
    address: String = "123 Mock St",
    city: String = "Mock City",
    zipCode: String = "12345",
    country: String = "FR",
    siret: String = "12345678901234",
    vat: String = "FR12345678901",
    description: String? = "Mock company description",
    siteUrl: String = "https://mockcompany.com",
    status: CompanyStatus = CompanyStatus.ACTIVE, // NEW PARAMETER
): CompanyEntity = transaction {
    CompanyEntity.new(id) {
        this.name = name
        this.address = address
        this.city = city
        this.zipCode = zipCode
        this.country = country
        this.siret = siret
        this.vat = vat
        this.description = description
        this.siteUrl = siteUrl
        this.status = status // NEW FIELD
    }
}
```

## Test Configuration

### H2 Database Schema
Tests automatically create the schema including the new `status` column through Exposed ORM initialization.

### Mock Module Setup
```kotlin
fun testModuleMocked() = module {
    includes(companyModule)
    // Test-specific configuration
}
```

## Test Coverage Goals

- **Route Level**: 100% coverage of new PUT/DELETE endpoints
- **Status Filtering**: All combinations of query + status parameters
- **Error Scenarios**: All 4xx error conditions tested
- **Data Integrity**: Relationship preservation verified
- **Backwards Compatibility**: Existing functionality unaffected

## Execution Strategy

Tests are written to fail initially (no implementation exists), following TDD principles. Implementation tasks will make these tests pass one by one:

1. Tests fail with "route not found" errors
2. Add route stubs → tests fail with "not implemented" 
3. Implement domain models → tests fail with serialization errors
4. Implement repository methods → tests fail with database errors
5. Add schema changes → tests fail with business logic errors
6. Complete implementation → tests pass

This ensures complete coverage and validates the contract implementation matches the specification.