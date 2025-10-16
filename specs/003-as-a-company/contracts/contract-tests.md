# Contract Tests: Job Offers API

## Test Suite Overview
These tests validate the API contracts defined in `api-contracts.md`. They should be implemented as integration tests that verify endpoint behavior without requiring full implementation.

## Test Categories

### 1. Create Job Offer Tests

#### POST /companies/{companyId}/job-offers

**Valid Request Test**:
```kotlin
@Test
fun `should create job offer with valid data`() {
    val companyId = createTestCompany()
    val request = CreateJobOffer(
        url = "https://example.com/jobs/developer",
        title = "Software Developer",
        location = "Paris, France", 
        publicationDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now().plusDays(30),
        experienceYears = 3,
        salary = "50000 EUR"
    )
    
    val response = client.post("/companies/$companyId/job-offers") {
        contentType(ContentType.Application.Json)
        setBody(request)
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.Created
    val body = response.body<Map<String, String>>()
    body["id"] shouldNotBe null
    UUID.fromString(body["id"]) // Should not throw
}
```

**Missing Required Fields Test**:
```kotlin
@Test
fun `should reject create request with missing required fields`() {
    val companyId = createTestCompany()
    val request = mapOf("title" to "Developer") // Missing url, location, publicationDate
    
    val response = client.post("/companies/$companyId/job-offers") {
        contentType(ContentType.Application.Json)
        setBody(request)
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.BadRequest
    val error = response.body<ErrorResponse>()
    error.error.code shouldBe "VALIDATION_ERROR"
}
```

**Invalid Date Validation Test**:
```kotlin
@Test
fun `should reject future publication date`() {
    val companyId = createTestCompany()
    val request = CreateJobOffer(
        url = "https://example.com/jobs/developer",
        title = "Software Developer",
        location = "Paris, France",
        publicationDate = LocalDate.now().plusDays(1), // Future date
        endDate = null,
        experienceYears = null,
        salary = null
    )
    
    val response = client.post("/companies/$companyId/job-offers") {
        contentType(ContentType.Application.Json)
        setBody(request)
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.BadRequest
}
```

### 2. List Job Offers Tests

#### GET /companies/{companyId}/job-offers

**Successful List Test**:
```kotlin
@Test
fun `should return paginated job offers for company`() {
    val companyId = createTestCompany()
    createTestJobOffer(companyId, "Developer 1")
    createTestJobOffer(companyId, "Developer 2")
    
    val response = client.get("/companies/$companyId/job-offers") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val body = response.body<PaginatedResponse<JobOfferResponse>>()
    body.items.size shouldBe 2
    body.pagination.totalItems shouldBe 2
    body.pagination.page shouldBe 1
}
```

**Pagination Test**:
```kotlin
@Test
fun `should handle pagination parameters`() {
    val companyId = createTestCompany()
    
    val response = client.get("/companies/$companyId/job-offers?page=2&page_size=5") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val body = response.body<PaginatedResponse<JobOfferResponse>>()
    body.pagination.page shouldBe 2
    body.pagination.pageSize shouldBe 5
}
```

### 3. Get Job Offer by ID Tests

#### GET /companies/{companyId}/job-offers/{jobOfferId}

**Successful Get Test**:
```kotlin
@Test
fun `should return job offer by ID`() {
    val companyId = createTestCompany()
    val jobOfferId = createTestJobOffer(companyId, "Test Developer")
    
    val response = client.get("/companies/$companyId/job-offers/$jobOfferId") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val jobOffer = response.body<JobOfferResponse>()
    jobOffer.id shouldBe jobOfferId
    jobOffer.companyId shouldBe companyId
    jobOffer.title shouldBe "Test Developer"
}
```

**Not Found Test**:
```kotlin
@Test
fun `should return 404 for non-existent job offer`() {
    val companyId = createTestCompany()
    val nonExistentId = UUID.randomUUID()
    
    val response = client.get("/companies/$companyId/job-offers/$nonExistentId") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.NotFound
}
```

### 4. Update Job Offer Tests

#### PUT /companies/{companyId}/job-offers/{jobOfferId}

**Successful Update Test**:
```kotlin
@Test
fun `should update job offer with valid data`() {
    val companyId = createTestCompany()
    val jobOfferId = createTestJobOffer(companyId, "Developer")
    val updateRequest = UpdateJobOffer(
        title = "Senior Developer",
        salary = "60000 EUR"
    )
    
    val response = client.put("/companies/$companyId/job-offers/$jobOfferId") {
        contentType(ContentType.Application.Json)
        setBody(updateRequest)
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val updated = response.body<JobOfferResponse>()
    updated.title shouldBe "Senior Developer"
    updated.salary shouldBe "60000 EUR"
    updated.updatedAt shouldNotBe updated.createdAt
}
```

**Partial Update Test**:
```kotlin
@Test
fun `should handle partial updates`() {
    val companyId = createTestCompany()
    val jobOfferId = createTestJobOffer(companyId, "Developer")
    val updateRequest = UpdateJobOffer(title = "Lead Developer")
    
    val response = client.put("/companies/$companyId/job-offers/$jobOfferId") {
        contentType(ContentType.Application.Json)
        setBody(updateRequest)
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.OK
    val updated = response.body<JobOfferResponse>()
    updated.title shouldBe "Lead Developer"
    // Other fields should remain unchanged
}
```

### 5. Delete Job Offer Tests

#### DELETE /companies/{companyId}/job-offers/{jobOfferId}

**Successful Delete Test**:
```kotlin
@Test
fun `should delete job offer successfully`() {
    val companyId = createTestCompany()
    val jobOfferId = createTestJobOffer(companyId, "Developer")
    
    val response = client.delete("/companies/$companyId/job-offers/$jobOfferId") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.NoContent
    
    // Verify deletion
    val getResponse = client.get("/companies/$companyId/job-offers/$jobOfferId") {
        bearerAuth(validToken)
    }
    getResponse.status shouldBe HttpStatusCode.NotFound
}
```

### 6. Authorization Tests

**Company Ownership Test**:
```kotlin
@Test
fun `should reject access to other company's job offers`() {
    val companyId1 = createTestCompany()
    val companyId2 = createTestCompany() 
    val jobOfferId = createTestJobOffer(companyId1, "Developer")
    
    // Try to access with wrong company ID
    val response = client.get("/companies/$companyId2/job-offers/$jobOfferId") {
        bearerAuth(validToken)
    }
    
    response.status shouldBe HttpStatusCode.NotFound // Or 403 depending on implementation
}
```

**Unauthenticated Access Test**:
```kotlin
@Test
fun `should reject unauthenticated requests`() {
    val companyId = createTestCompany()
    
    val response = client.get("/companies/$companyId/job-offers")
    
    response.status shouldBe HttpStatusCode.Unauthorized
}
```

## Test Data Helpers

```kotlin
private fun createTestCompany(): UUID {
    // Create a test company and return its ID
    // Implementation depends on existing test infrastructure
}

private fun createTestJobOffer(companyId: UUID, title: String): UUID {
    // Create a test job offer and return its ID
    // Implementation depends on existing test infrastructure
}

private val validToken: String
    get() = generateValidJWTToken() // Implementation depends on auth system
```

## Test Configuration

### Database Setup
- Use H2 in-memory database for tests
- Reset database state between tests
- Ensure referential integrity with Companies table

### Authentication Setup
- Mock JWT token generation for valid authentication
- Test different user permissions and company ownership scenarios
- Validate authorization boundaries

### Error Handling Tests
- Verify consistent error response format
- Test all defined HTTP status codes
- Validate error message clarity and actionability

These contract tests should be implemented first (TDD approach) and will initially fail until the actual implementation is complete. They serve as both specification validation and regression prevention.