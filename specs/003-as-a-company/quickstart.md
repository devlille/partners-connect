# Quickstart: Job Offers Management Implementation

## Overview
This quickstart guide provides step-by-step validation of the job offers management feature. Follow these steps to verify the implementation meets all requirements.

## Prerequisites
- Kotlin/JVM 21 environment
- PostgreSQL database running (or H2 for tests)
- Authentication system configured
- Existing company in the system for testing

## Development Environment Setup

### 1. Database Schema Verification
```bash
# Navigate to server directory
cd server

# Run database migrations to create job offers table
./gradlew run --no-daemon

# Verify table creation in PostgreSQL
psql -h localhost -U your_user -d partners_connect
\d company_job_offers
```

Expected output should show:
- `id` (UUID, primary key)
- `company_id` (UUID, foreign key to companies)
- `url`, `title`, `location` (varchar fields)
- `publication_date`, `end_date` (date fields)
- `experience_years` (integer, nullable)
- `salary` (varchar, nullable)
- `created_at`, `updated_at` (timestamp fields)

### 2. API Contract Validation
```bash
# Run contract tests
./gradlew test --tests "*JobOfferContract*" --no-daemon

# Run full test suite
./gradlew test --no-daemon
```

### 3. OpenAPI Documentation Check
```bash
# Validate OpenAPI specification
cd /path/to/project/root
npm run validate

# Generate documentation (if needed)
npm run docs
```

## Functional Testing Scenarios

### Scenario 1: Create Job Offer (Happy Path)

**Given**: I am authenticated as a company owner  
**When**: I create a job offer with valid data  
**Then**: The job offer is created successfully

```bash
# Example HTTP request (using httpie or curl)
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="https://example.com/jobs/kotlin-developer" \
  title="Senior Kotlin Developer" \
  location="Lille, France" \
  publicationDate="2025-10-16" \
  endDate="2025-12-15" \
  experienceYears:=5 \
  salary="60000-70000 EUR"
```

**Expected Response** (201 Created):
```json
{
  "id": "generated-uuid-here"
}
```

### Scenario 2: List Company Job Offers

**Given**: I have created job offers for my company  
**When**: I retrieve the job offers list  
**Then**: I see all my company's job offers

```bash
http GET localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN"
```

**Expected Response** (200 OK):
```json
{
  "items": [
    {
      "id": "uuid",
      "companyId": "company-uuid",
      "url": "https://example.com/jobs/kotlin-developer",
      "title": "Senior Kotlin Developer",
      "location": "Lille, France",
      "publicationDate": "2025-10-16",
      "endDate": "2025-12-15",
      "experienceYears": 5,
      "salary": "60000-70000 EUR",
      "createdAt": "2025-10-16T10:30:00.000Z",
      "updatedAt": "2025-10-16T10:30:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 1,
    "totalPages": 1
  }
}
```

### Scenario 3: Update Job Offer

**Given**: I have a job offer  
**When**: I update its details  
**Then**: The changes are saved

```bash
http PUT localhost:8080/companies/{companyId}/job-offers/{jobOfferId} \
  Authorization:"Bearer $JWT_TOKEN" \
  title="Lead Kotlin Developer" \
  salary="70000-85000 EUR"
```

**Expected Response** (200 OK): Full job offer object with updated fields and new `updatedAt` timestamp.

### Scenario 4: Delete Job Offer

**Given**: I have a job offer  
**When**: I delete it  
**Then**: It is removed from the system

```bash
http DELETE localhost:8080/companies/{companyId}/job-offers/{jobOfferId} \
  Authorization:"Bearer $JWT_TOKEN"
```

**Expected Response** (204 No Content): Empty response body.

## Validation Test Cases

### 1. Data Validation Tests

**Test Invalid URL**:
```bash
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="not-a-valid-url" \
  title="Developer" \
  location="Paris" \
  publicationDate="2025-10-16"
```
**Expected**: 400 Bad Request with validation error

**Test Future Publication Date**:
```bash
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="https://example.com/job" \
  title="Developer" \
  location="Paris" \
  publicationDate="2025-12-01"  # Future date
```
**Expected**: 400 Bad Request with date validation error

**Test Invalid Experience Years**:
```bash
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="https://example.com/job" \
  title="Developer" \
  location="Paris" \
  publicationDate="2025-10-16" \
  experienceYears:=25  # Above maximum of 20
```
**Expected**: 400 Bad Request with range validation error

### 2. Authorization Tests

**Test Wrong Company Access**:
```bash
# Try to access job offers of a different company
http GET localhost:8080/companies/{differentCompanyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN"
```
**Expected**: 403 Forbidden or 404 Not Found

**Test Unauthenticated Access**:
```bash
http GET localhost:8080/companies/{companyId}/job-offers
```
**Expected**: 401 Unauthorized

### 3. Edge Cases

**Test End Date Before Publication Date**:
```bash
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="https://example.com/job" \
  title="Developer" \
  location="Paris" \
  publicationDate="2025-10-20" \
  endDate="2025-10-15"  # Before publication date
```
**Expected**: 400 Bad Request with date logic validation error

**Test Empty Optional Fields**:
```bash
http POST localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  url="https://example.com/job" \
  title="Developer" \
  location="Paris" \
  publicationDate="2025-10-16"
  # No optional fields provided
```
**Expected**: 201 Created (optional fields should be null)

## Performance Validation

### 1. Response Time Check
```bash
# Time the API responses
time http GET localhost:8080/companies/{companyId}/job-offers \
  Authorization:"Bearer $JWT_TOKEN"
```
**Expected**: Response time < 2 seconds (constitutional requirement)

### 2. Pagination Performance
```bash
# Test with large dataset (if available)
http GET localhost:8080/companies/{companyId}/job-offers?page=1&page_size=100 \
  Authorization:"Bearer $JWT_TOKEN"
```
**Expected**: Efficient query execution without timeout

## Database Verification

### 1. Data Persistence Check
After creating a job offer:
```sql
SELECT * FROM company_job_offers WHERE company_id = 'your-company-uuid';
```
**Expected**: Record exists with correct foreign key relationship

### 2. Constraint Validation
```sql
-- Try to insert invalid data directly (should fail)
INSERT INTO company_job_offers (id, company_id, url, title, location, publication_date, experience_years) 
VALUES (gen_random_uuid(), 'valid-company-uuid', 'https://example.com', 'Title', 'Location', CURRENT_DATE, 25);
```
**Expected**: Constraint violation error (experience_years > 20)

## Integration Testing

### 1. Partnership Promotion (Future Feature)
This will be validated when partnership promotion is implemented:
```bash
# Test promoting job offers to active partnerships
http POST localhost:8080/companies/{companyId}/partnerships/{partnershipId}/promote-job-offers \
  Authorization:"Bearer $JWT_TOKEN" \
  jobOfferIds:='["job-offer-uuid-1", "job-offer-uuid-2"]'
```

### 2. Company Deletion Impact
Test what happens when a company is deleted:
```sql
-- Check cascade behavior
DELETE FROM companies WHERE id = 'test-company-uuid';
SELECT COUNT(*) FROM company_job_offers WHERE company_id = 'test-company-uuid';
```
**Expected**: Job offers should be handled according to foreign key constraints

## Success Criteria Checklist

- [ ] All HTTP endpoints respond with correct status codes
- [ ] Request/response formats match API contracts
- [ ] Data validation works for all required and optional fields
- [ ] Authentication and authorization work correctly
- [ ] Database constraints prevent invalid data
- [ ] Response times meet performance requirements (<2 seconds)
- [ ] Pagination works correctly
- [ ] Error responses follow consistent format
- [ ] Optional fields are handled properly (null values)
- [ ] Date validation prevents logical inconsistencies
- [ ] Company ownership is enforced for all operations
- [ ] Created and updated timestamps are maintained
- [ ] Foreign key relationships are preserved

## Troubleshooting

### Common Issues

**Database Connection Issues**:
- Verify PostgreSQL is running
- Check connection configuration in docker-compose.yml
- Ensure database user has proper permissions

**Authentication Failures**:
- Verify JWT token is valid and not expired
- Check company ownership in the token claims
- Ensure authentication middleware is configured

**Validation Errors**:
- Check JSON schema files are properly loaded
- Verify request Content-Type is application/json
- Ensure all required fields are provided

**Performance Issues**:
- Check database indexes are created
- Monitor query execution plans
- Verify connection pooling is configured

This quickstart guide should be executed after implementation to verify all functionality works as specified. Each test should pass before considering the feature complete.