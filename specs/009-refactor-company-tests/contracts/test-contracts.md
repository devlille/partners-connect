# Test Contracts: Company Test Codebase Refactoring

## Contract Test Specifications

### Contract Test File: CompanyCreateContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyCreateContractTest.kt`

**Purpose**: Validate POST /companies API schema and HTTP compliance

**Test Scenarios**:
```kotlin
@Test
fun `POST companies should accept valid CreateCompany request and return 201 with ID`()

@Test  
fun `POST companies should return 400 for missing required fields`()

@Test
fun `POST companies should return 400 for invalid field formats`()

@Test
fun `POST companies should validate address schema correctly`()

@Test
fun `POST companies should validate social media schema correctly`()
```

**Schema Validation Focus**:
- CreateCompany request body validation
- HTTP 201 response with company ID
- HTTP 400 for validation errors
- Address nested object validation
- Social media array validation

---

### Contract Test File: CompanyGetContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyGetContractTest.kt`

**Purpose**: Validate GET /companies/{id} API schema and HTTP compliance

**Test Scenarios**:
```kotlin
@Test
fun `GET company by ID should return 200 with complete company schema`()

@Test
fun `GET company by ID should return 404 for non-existent company`()

@Test
fun `GET company by ID should return 400 for invalid UUID format`()

@Test
fun `GET company response should include all required fields`()

@Test
fun `GET company response should serialize nested objects correctly`()
```

**Schema Validation Focus**:
- Company response schema validation
- HTTP 200 for valid requests
- HTTP 404 for missing entities
- HTTP 400 for invalid parameters
- Nested object serialization

---

### Contract Test File: CompanyListContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyListContractTest.kt`

**Purpose**: Validate GET /companies API schema and pagination

**Test Scenarios**:
```kotlin
@Test
fun `GET companies should return 200 with paginated response schema`()

@Test
fun `GET companies should validate pagination parameters`()

@Test
fun `GET companies should handle empty results correctly`()

@Test
fun `GET companies should validate query parameters`()

@Test
fun `GET companies should return consistent pagination metadata`()
```

**Schema Validation Focus**:
- Paginated response schema validation
- Pagination parameter validation
- Empty result handling
- Query parameter validation
- Pagination metadata consistency

---

### Contract Test File: CompanyUpdateContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyUpdateContractTest.kt`

**Purpose**: Validate PUT /companies/{id} API schema and HTTP compliance

**Test Scenarios**:
```kotlin
@Test
fun `PUT company should accept valid UpdateCompany request and return 200`()

@Test
fun `PUT company should return 404 for non-existent company`()

@Test
fun `PUT company should return 400 for invalid field formats`()

@Test
fun `PUT company should validate partial updates correctly`()

@Test
fun `PUT company should maintain schema consistency`()
```

**Schema Validation Focus**:
- UpdateCompany request body validation
- HTTP 200 for successful updates
- HTTP 404 for missing entities
- Partial update validation
- Response schema consistency

---

### Contract Test File: CompanyLogoUploadContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyLogoUploadContractTest.kt`

**Purpose**: Validate POST /companies/{id}/logo file upload API

**Test Scenarios**:
```kotlin
@Test
fun `POST logo should accept valid image files and return 200`()

@Test
fun `POST logo should return 415 for unsupported file types`()

@Test
fun `POST logo should return 404 for non-existent company`()

@Test
fun `POST logo should validate file size limits`()

@Test
fun `POST logo should return proper upload response schema`()
```

**Schema Validation Focus**:
- Multipart form data validation
- File type validation (415 status)
- File size validation
- Upload response schema
- Error response formats

---

### Contract Test File: CompanyJobOfferContractTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferContractTest.kt`

**Purpose**: Validate job offer CRUD API schemas

**Test Scenarios**:
```kotlin
@Test
fun `POST job offer should validate CreateJobOffer schema`()

@Test
fun `GET job offer should validate JobOffer response schema`()

@Test
fun `PUT job offer should validate UpdateJobOffer schema`()

@Test
fun `DELETE job offer should return proper status codes`()

@Test
fun `GET job offers list should validate pagination schema`()
```

**Schema Validation Focus**:
- CreateJobOffer/UpdateJobOffer request validation
- JobOffer response schema validation  
- CRUD operation status codes
- Job offer list pagination
- Error response consistency

---

## Integration Test Specifications

### Integration Test File: CompanyLifecycleIntegrationTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyLifecycleIntegrationTest.kt`

**Purpose**: Validate end-to-end company management workflows

**Test Scenarios**:
```kotlin
@Test
fun `should create company with complete business validation`()

@Test
fun `should update company with business rule enforcement`()

@Test
fun `should handle company status transitions correctly`()

@Test
fun `should maintain data consistency across updates`()

@Test
fun `should enforce business constraints during lifecycle`()
```

**Business Logic Focus**:
- Complete company creation workflow
- Business rule validation
- Status transition logic
- Data consistency checks
- Constraint enforcement

---

### Integration Test File: CompanyJobOfferManagementIntegrationTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyJobOfferManagementIntegrationTest.kt`

**Purpose**: Validate job offer business workflows and company relationships

**Test Scenarios**:
```kotlin
@Test
fun `should manage job offer lifecycle with business rules`()

@Test
fun `should handle job offer promotion workflows`()

@Test
fun `should maintain company-job offer relationships`()

@Test
fun `should validate job offer business constraints`()

@Test
fun `should support job offer search and filtering`()
```

**Business Logic Focus**:
- Job offer business workflows
- Promotion business logic
- Company-job offer relationships
- Business constraint validation
- Search/filtering functionality

---

### Integration Test File: CompanyPartnershipWorkflowIntegrationTest.kt

**Location**: `server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyPartnershipWorkflowIntegrationTest.kt`

**Purpose**: Validate cross-domain interactions with partnership module

**Test Scenarios**:
```kotlin
@Test
fun `should integrate with partnership creation workflows`()

@Test
fun `should handle partnership status changes affecting companies`()

@Test
fun `should maintain company data consistency in partnerships`()

@Test
fun `should validate cross-domain business rules`()

@Test
fun `should support partnership-related company operations`()
```

**Business Logic Focus**:
- Cross-domain integration
- Partnership workflow integration
- Data consistency across modules
- Cross-domain business rules
- Partnership-related operations

## Test Execution Contracts

### Performance Requirements

**Contract Tests**:
- Execution time: <2 seconds total
- Database interactions: Minimal (schema validation only)
- Setup complexity: Single entity creation via factories

**Integration Tests**:
- Execution time: <2 seconds total  
- Database interactions: Full business workflows
- Setup complexity: Multi-entity relationships via factories

### Coverage Requirements

**Contract Test Coverage**:
- 100% of API endpoint request/response schemas
- All HTTP status code scenarios
- Request validation scenarios
- Error response format validation

**Integration Test Coverage**:
- 95%+ of existing business logic scenarios
- End-to-end workflow validation
- Cross-cutting concern validation
- Business rule enforcement validation

### Quality Contracts

**Code Quality**:
- ktlint compliance for all test files
- detekt compliance for all test files
- Meaningful test method names
- Clear test documentation

**Test Organization**:
- Clear separation of contract vs integration concerns
- Consistent naming conventions
- Proper package organization
- Factory function reuse