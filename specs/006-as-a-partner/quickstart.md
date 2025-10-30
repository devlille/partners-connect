# Quickstart: Complete CRUD Operations for Companies

## Overview
This guide demonstrates the complete CRUD functionality for the `/companies` resource, including the new update and soft delete operations with status filtering.

## Prerequisites
- Server running on `http://localhost:8080`
- Companies exist in the database (see Setup section)
- HTTP client (curl, Postman, or similar)

## Setup Test Data

### Create a Test Company
```bash
curl -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Company",
    "site_url": "https://test-company.com",
    "head_office": {
      "address": "123 Test Street",
      "city": "Paris",
      "zip_code": "75001",
      "country": "FR"
    },
    "siret": "12345678901234",
    "vat": "FR12345678901",
    "description": "A test company for demonstration"
  }'
```

**Expected Response** (201 Created):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Save the company ID** from the response for the following operations.

## CRUD Operations Demo

### 1. READ - List All Companies (Enhanced with Status)

```bash
curl -X GET http://localhost:8080/companies
```

**Expected Response** (200 OK):
```json
{
  "items": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Test Company",
      "site_url": "https://test-company.com",
      "head_office": {
        "address": "123 Test Street",
        "city": "Paris",
        "zip_code": "75001",
        "country": "FR"
      },
      "siret": "12345678901234",
      "vat": "FR12345678901",
      "description": "A test company for demonstration",
      "medias": null,
      "status": "active"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```

**Key Observation**: Notice the new `status` field showing `"active"` for all existing companies.

### 2. READ - Get Specific Company

```bash
curl -X GET http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000
```

**Expected Response** (200 OK): Same company object as above with `status: "active"`.

### 3. UPDATE - Partial Company Information (NEW)

Update only the company name and description:

```bash
curl -X PUT http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Test Company",
    "description": "Updated description with new information"
  }'
```

**Expected Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Updated Test Company",
  "site_url": "https://test-company.com",
  "head_office": {
    "address": "123 Test Street",
    "city": "Paris",
    "zip_code": "75001",
    "country": "FR"
  },
  "siret": "12345678901234",
  "vat": "FR12345678901",
  "description": "Updated description with new information",
  "medias": null,
  "status": "active"
}
```

**Key Observation**: Only `name` and `description` changed. All other fields (`site_url`, `head_office`, `siret`, `vat`) remained unchanged.

### 4. UPDATE - Complete Information Update

Update multiple fields at once:

```bash
curl -X PUT http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Comprehensive Updated Company",
    "site_url": "https://updated-company.com",
    "head_office": {
      "address": "456 Updated Avenue",
      "city": "Lyon",
      "zip_code": "69001",
      "country": "FR"
    },
    "description": "Completely updated company information",
    "socials": [
      {
        "type": "TWITTER",
        "url": "https://twitter.com/updated_company"
      },
      {
        "type": "LINKEDIN",
        "url": "https://linkedin.com/company/updated-company"
      }
    ]
  }'
```

**Expected Response** (200 OK): Updated company with all new information and `status`: `"active"`.

### 5. DELETE - Soft Delete Company (NEW)

```bash
curl -X DELETE http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000
```

**Expected Response** (204 No Content): Empty response body.

### 6. Verify Soft Delete - Company Still Accessible

```bash
curl -X GET http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000
```

**Expected Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Comprehensive Updated Company",
  "site_url": "https://updated-company.com",
  "head_office": {
    "address": "456 Updated Avenue",
    "city": "Lyon",
    "zip_code": "69001",
    "country": "FR"
  },
  "siret": "12345678901234",
  "vat": "FR12345678901",
  "description": "Completely updated company information",
  "medias": null,
  "status": "inactive"
}
```

**Key Observation**: Company data is preserved but `status` is now `"inactive"`.

## Status Filtering Demo

### Create Additional Test Data

First, create another company to demonstrate filtering:

```bash
curl -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Active Company",
    "site_url": "https://active-company.com",
    "head_office": {
      "address": "789 Active Street",
      "city": "Marseille",
      "zip_code": "13001",
      "country": "FR"
    },
    "siret": "98765432109876",
    "vat": "FR98765432109",
    "description": "An active company"
  }'
```

### 1. List All Companies (Default Behavior)

```bash
curl -X GET http://localhost:8080/companies
```

**Expected Response**: Shows both companies (1 active, 1 inactive) with their respective statuses.

### 2. Filter Active Companies Only

```bash
curl -X GET "http://localhost:8080/companies?status=active"
```

**Expected Response**: Shows only the second company with `status`: `"active"`.

### 3. Filter Inactive Companies Only

```bash
curl -X GET "http://localhost:8080/companies?status=inactive"
```

**Expected Response**: Shows only the first company with `status`: `"inactive"`.

### 4. Combine Search with Status Filter

```bash
curl -X GET "http://localhost:8080/companies?query=active&status=active"
```

**Expected Response**: Shows only companies matching "active" in the name AND having active status.

## Error Scenarios Demo

### 1. Update Non-Existent Company

```bash
curl -X PUT http://localhost:8080/companies/00000000-0000-0000-0000-000000000000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "This will fail"
  }'
```

**Expected Response** (404 Not Found):
```json
{
  "error": "not_found",
  "message": "Company not found"
}
```

### 2. Invalid Update Data

```bash
curl -X PUT http://localhost:8080/companies/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{
    "siret": "invalid-siret",
    "vat": "invalid-vat"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "error": "validation_failed",
  "message": "Request validation failed",
  "details": {
    "siret": "Invalid SIRET format - must be 14 digits",
    "vat": "Invalid VAT format"
  }
}
```

### 3. Delete Non-Existent Company

```bash
curl -X DELETE http://localhost:8080/companies/00000000-0000-0000-0000-000000000000
```

**Expected Response** (404 Not Found):
```json
{
  "error": "not_found", 
  "message": "Company not found"
}
```

### 4. Invalid Status Filter

```bash
curl -X GET "http://localhost:8080/companies?status=invalid"
```

**Expected Response** (400 Bad Request):
```json
{
  "error": "validation_failed",
  "message": "Invalid status parameter",
  "details": {
    "status": "Must be one of: active, inactive"
  }
}
```

## Data Integrity Verification

### Create Partnership to Test Relationship Preservation

If you have partnership functionality available:

```bash
# Create a partnership linked to the inactive company
curl -X POST http://localhost:8080/partnerships \
  -H "Content-Type: application/json" \
  -d '{
    "company_id": "123e4567-e89b-12d3-a456-426614174000",
    "event_slug": "test-event",
    "pack_name": "gold"
  }'
```

**Verify**: The partnership should still exist and reference the inactive company, demonstrating that soft delete preserves relationships.

## Backwards Compatibility Verification

### Test Existing API Behavior

All existing API calls should continue to work with additional `status` field in responses:

1. **GET /companies** - Now returns companies with status field
2. **GET /companies/{id}** - Now returns company with status field  
3. **POST /companies** - Creates companies with default active status
4. **POST /companies/{id}/logo** - Logo upload still works for all companies

## Performance Testing

### Large Dataset Filtering

If you have many companies, test filtering performance:

```bash
# Time the request to ensure <2 second response requirement
time curl -X GET "http://localhost:8080/companies?status=active&page=1&page_size=100"
```

**Expected**: Response time should be under 2 seconds even with filtering.

## Success Criteria Checklist

After running this quickstart, verify:

- [ ] ✅ **CR**eate: POST /companies works (unchanged)
- [ ] ✅ **R**ead: GET /companies includes status field
- [ ] ✅ **R**ead: GET /companies/{id} includes status field
- [ ] ✅ **U**pdate: PUT /companies/{id} updates companies (NEW)
- [ ] ✅ **D**elete: DELETE /companies/{id} soft deletes (NEW)
- [ ] ✅ Status filtering works (active, inactive, all)
- [ ] ✅ Default behavior shows all companies
- [ ] ✅ Soft delete preserves data and relationships
- [ ] ✅ Partial updates work correctly
- [ ] ✅ Error handling returns proper status codes
- [ ] ✅ Validation errors provide clear messages
- [ ] ✅ Backwards compatibility maintained

## Troubleshooting

### Common Issues

**Issue**: 500 Internal Server Error on new endpoints
**Solution**: Check that schema migration completed and `status` column exists

**Issue**: Status filter not working
**Solution**: Verify enum values match exactly: `"active"`, `"inactive"`  

**Issue**: Tests failing
**Solution**: Ensure H2 database has the updated schema with status column

**Issue**: Validation errors on update
**Solution**: Check that UpdateCompany model matches schema expectations

This quickstart validates the complete CRUD implementation and demonstrates all functional requirements from the specification.