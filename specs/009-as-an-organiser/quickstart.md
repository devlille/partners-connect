# Quickstart: Provider Management Enhancement

## Test Scenario Validation

This quickstart validates the complete provider management workflow from creation to event attachment following the user stories from the feature specification.

### Prerequisites

1. **Test Environment Setup**
   ```bash
   cd server
   ./gradlew test --no-daemon  # Run tests to verify H2 database setup
   ```

2. **Test Data Setup**
   ```kotlin
   // Mock factories for consistent test data
   val testOrg = mockOrganisation(slug = "test-org") 
   val testEvent = mockEvent(orgSlug = "test-org", slug = "test-event")
   val testUser = mockUser(email = "organiser@test-org.com")
   val testProvider = mockProvider(organisationId = testOrg.id)
   ```

### Scenario 1: Organisation-Scoped Provider Management

**Given**: An organiser is logged in to their organisation  
**When**: They create a new provider  
**Then**: The provider is automatically linked to their organisation

```http
POST /orgs/test-org/providers
Authorization: Bearer <valid_jwt_for_organiser>
Content-Type: application/json

{
  "name": "Test Catering Service",
  "type": "catering", 
  "website": "https://test-catering.com",
  "phone": "+33123456789",
  "email": "contact@test-catering.com"
}

Expected Response: 201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Test Catering Service", 
  "type": "catering",
  "website": "https://test-catering.com",
  "phone": "+33123456789", 
  "email": "contact@test-catering.com",
  "org_slug": "test-org",
  "created_at": "2025-11-08T15:00:00Z"
}
```

**Validation Steps:**
1. Provider created with correct organisation association
2. Provider appears in organisation provider listing
3. Provider not accessible from different organisation scope

### Scenario 2: Provider Event Attachment

**Given**: An organiser has providers in their organisation  
**When**: They attach a provider to an event  
**Then**: The provider becomes available for that event

```http
POST /orgs/test-org/events/test-event/providers  
Authorization: Bearer <valid_jwt_for_organiser>
Content-Type: application/json

["550e8400-e29b-41d4-a716-446655440000"]

Expected Response: 201 Created
{
  "attached_providers": [
    {
      "provider_id": "550e8400-e29b-41d4-a716-446655440000",
      "event_slug": "test-event", 
      "attached_at": "2025-11-08T15:00:00Z"
    }
  ]
}
```

**Validation Steps:**
1. Provider attached to event successfully
2. Provider appears in event provider listing
3. Same provider can be attached to multiple events
4. Provider from different organisation cannot be attached

### Scenario 3: Cascading Provider Deletion

**Given**: A provider is attached to multiple events  
**When**: An organiser deletes the provider  
**Then**: The provider is first detached from all events before being deleted

```http
# Step 1: Attempt deletion while still attached (should fail)
DELETE /orgs/test-org/providers/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <valid_jwt_for_organiser>

Expected Response: 409 Conflict
{
  "message": "Provider is still attached to events and cannot be deleted"
}

# Step 2: Detach from all events
DELETE /orgs/test-org/events/test-event/providers
Authorization: Bearer <valid_jwt_for_organiser>
Content-Type: application/json

["550e8400-e29b-41d4-a716-446655440000"]

Expected Response: 204 No Content

# Step 3: Delete provider (should succeed)  
DELETE /orgs/test-org/providers/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <valid_jwt_for_organiser>

Expected Response: 204 No Content
```

**Validation Steps:**
1. Provider deletion blocked while attached to events
2. Provider detachment removes event association
3. Provider deletion succeeds after all detachments  
4. Provider no longer appears in any listings

### Scenario 4: Organisation-Filtered Public Listing

**Given**: A user visits the public provider list  
**When**: They filter by organisation slug  
**Then**: They see only providers belonging to that organisation

```http
GET /providers?org_slug=test-org&query=catering&sort=name&page=1&page_size=20

Expected Response: 200 OK
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Test Catering Service",
      "type": "catering", 
      "website": "https://test-catering.com",
      "phone": "+33123456789",
      "email": "contact@test-catering.com", 
      "org_slug": "test-org",
      "created_at": "2025-11-08T15:00:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```

**Validation Steps:**
1. Public listing works without authentication
2. Organisation filtering returns only relevant providers
3. Existing query parameters (query, sort, page) preserved
4. Pagination metadata follows standard format (items/page/page_size/total)

### Scenario 5: Permission Boundary Testing

**Given**: An organiser from organisation A  
**When**: They try to manage providers from organisation B  
**Then**: Access is denied with appropriate error

```http
POST /orgs/other-org/providers
Authorization: Bearer <jwt_for_test-org_organiser>
Content-Type: application/json

{
  "name": "Unauthorized Provider",
  "type": "catering"
}

Expected Response: 403 Forbidden  
{
  "message": "User does not have permission to access this organisation"
}
```

**Validation Steps:**
1. Cross-organisation access properly blocked
2. Error messages clear and actionable
3. AuthorizedOrganisationPlugin working correctly
4. No data leakage between organisations

## Performance Validation

### Database Query Optimization
```sql
-- Verify indexes created properly
EXPLAIN ANALYZE SELECT p.* FROM providers p 
JOIN organisations o ON p.organisation_id = o.id 
WHERE o.slug = 'test-org' AND p.name ILIKE '%catering%'
ORDER BY p.name LIMIT 20;

-- Expected: Index scan on idx_providers_organisation_id
```

### Response Time Requirements
```bash
# Measure response times for critical operations
curl -w "@curl-format.txt" -H "Authorization: Bearer <token>" \
  "http://localhost:8080/orgs/test-org/providers"

# Expected: < 2 seconds for all operations
```

## Integration Test Coverage

### Contract Tests (Schema Validation Only)
- ✅ POST /orgs/{orgSlug}/providers validates create_provider.schema.json
- ✅ PUT /orgs/{orgSlug}/providers/{id} validates update_provider.schema.json  
- ✅ POST /orgs/{orgSlug}/events/{eventSlug}/providers validates create_by_identifiers.schema.json
- ✅ GET responses match provider.schema.json and paginated_provider.schema.json
- ✅ Error responses follow standardized ResponseException format (message + optional stack)

### Integration Tests (End-to-End Business Logic)
- ✅ Provider CRUD with organisation scoping
- ✅ Provider-event attachment workflows
- ✅ Cascading deletion with event detachment
- ✅ Permission validation across organisation boundaries
- ✅ Public provider listing with filtering
- ✅ Database constraint enforcement

### Mock Factory Usage
```kotlin
// Reusable test data factories
fun mockProvider(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Provider",
    type: String = "catering",
    organisationId: UUID = mockOrganisation().id
): Provider

fun mockCreateProviderRequest(
    name: String = "Test Provider", 
    type: String = "catering",
    website: String? = "https://test.com"
): CreateProviderRequest

fun mockAttachProvidersRequest(
    providerIds: List<UUID> = listOf(UUID.randomUUID())
): List<String>  // Array of UUID strings for bulk attachment
```

## Success Criteria

- [ ] All API contracts implemented and tested
- [ ] Database migration applied successfully  
- [ ] Organisation-scoped permissions enforced
- [ ] Provider-event attachment workflows functional
- [ ] Cascading deletion works properly
- [ ] Public listing with filtering operational
- [ ] 80%+ test coverage achieved
- [ ] Response times under 2 seconds
- [ ] Zero ktlint/detekt violations
- [ ] OpenAPI documentation updated