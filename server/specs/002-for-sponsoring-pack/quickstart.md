# Quickstart: Multi-Language Sponsoring Pack and Option Management for Organizers

**Feature**: Remove Accept-Language header dependency from organizer-facing sponsoring endpoints  
**Quickstart Date**: October 4, 2025  
**Prerequisites**: Running server with authentication, test organization and event created

## Development Environment Setup

### Prerequisites Checklist
- [ ] Kotlin/JVM 21 installed
- [ ] PostgreSQL running locally (or H2 for testing)
- [ ] Server running on localhost:8080
- [ ] Valid authentication token for organization management
- [ ] Test organization with slug "test-org"
- [ ] Test event with slug "test-event-2025"

### Server Startup
```bash
cd server
./gradlew run --no-daemon
```

**Expected**: Server starts on port 8080 without errors

## Basic Functionality Tests

### Test 1: Organizer Packs Endpoint (Multi-Language)

**Setup**: Create sponsoring options with multiple translations
```bash
# This test assumes options with translations already exist
# The endpoint should return all translation data
```

**Test Request**:
```bash
curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/packs" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response Structure**:
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Gold Sponsor",
    "base_price": 50000,
    "max_quantity": 5,
    "required_options": [
      {
        "id": "456e7890-e89b-12d3-a456-426614174000",
        "translations": {
          "en": {
            "language": "en",
            "name": "Logo on website",
            "description": "Company logo displayed on event website"
          },
          "fr": {
            "language": "fr", 
            "name": "Logo sur le site web",
            "description": "Logo de l'entreprise affiché sur le site de l'événement"
          }
        },
        "price": null
      }
    ],
    "optional_options": []
  }
]
```

**Key Validation Points**:
- [ ] No Accept-Language header required
- [ ] Response includes all available translations per option
- [ ] Translation objects include language, name, description fields
- [ ] Empty translations map handled gracefully
- [ ] Authentication still required

### Test 2: Organizer Options Endpoint (Multi-Language)

**Test Request**:
```bash
curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/options" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response Structure**:
```json
[
  {
    "id": "456e7890-e89b-12d3-a456-426614174000",
    "translations": {
      "en": {
        "language": "en",
        "name": "Logo on website", 
        "description": "Company logo displayed on event website"
      },
      "fr": {
        "language": "fr",
        "name": "Logo sur le site web",
        "description": "Logo de l'entreprise affiché sur le site de l'événement"
      },
      "de": {
        "language": "de",
        "name": "Logo auf Website",
        "description": null
      }
    },
    "price": null
  }
]
```

**Key Validation Points**:
- [ ] No Accept-Language header required
- [ ] All translations returned for each option
- [ ] Partial translations handled (missing descriptions OK)
- [ ] Different languages per option supported

### Test 3: Backward Compatibility (Public Endpoints)

**Test Request** (Public endpoint should still require Accept-Language):
```bash
curl -X GET "http://localhost:8080/events/test-event-2025/sponsoring/packs" \
  -H "Accept-Language: en"
```

**Expected**:
- [ ] Still requires Accept-Language header
- [ ] Returns single-language response format
- [ ] No breaking changes to public API

**Test Request** (Public endpoint without Accept-Language should fail):
```bash
curl -X GET "http://localhost:8080/events/test-event-2025/sponsoring/packs"
```

**Expected**:
- [ ] Returns 400 Bad Request
- [ ] Error message about missing Accept-Language header

## Edge Case Testing

### Test 4: Options with No Translations

**Scenario**: Option exists but has no translation records

**Expected Response**:
```json
{
  "id": "999e4567-e89b-12d3-a456-426614174000", 
  "translations": {},
  "price": 25000
}
```

**Validation**:
- [ ] Empty translations map returned (not null)
- [ ] Option still appears in response
- [ ] No server error

### Test 5: Options with Incomplete Translations

**Scenario**: Option has some translations missing name or description

**Expected Behavior**:
- [ ] Only complete translations included in response
- [ ] Incomplete translations skipped (not returned with null/empty values)
- [ ] Server handles gracefully without errors

### Test 6: Authentication and Authorization

**Test without authentication**:
```bash
curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/packs"
```

**Expected**: 401 Unauthorized

**Test with wrong organization permissions**:
```bash
curl -X GET "http://localhost:8080/orgs/unauthorized-org/events/test-event-2025/packs" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: 403 Forbidden

## Performance Validation

### Test 7: Response Time Verification

**Setup**: Event with multiple packs and options with translations

**Test**:
```bash
time curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/packs" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**:
- [ ] Response time < 2 seconds (constitution requirement)
- [ ] No significant degradation vs single-language responses
- [ ] Memory usage remains reasonable

## Integration Testing

### Test 8: End-to-End Organizer Workflow

**Scenario**: Organizer reviewing options to identify missing translations

1. **List all options**:
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/options" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

2. **Identify options needing translations**:
   - [ ] Options with empty translations map
   - [ ] Options missing specific language codes
   - [ ] Options with incomplete description translations

3. **Verify pack-embedded options match**:
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/test-event-2025/packs" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```
   - [ ] Options in packs have same translation data as direct options endpoint
   - [ ] Consistent data structure between endpoints

## Error Scenario Testing  

### Test 9: Invalid Event/Organization

**Test Request**:
```bash
curl -X GET "http://localhost:8080/orgs/nonexistent-org/events/fake-event/packs" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**:
- [ ] 404 Not Found response
- [ ] Clear error message
- [ ] No server crash or 500 errors

### Test 10: Database Connection Issues

**Test**: Temporarily stop database (development testing only)

**Expected**:
- [ ] Appropriate 500 error response
- [ ] Error logging with correlation IDs
- [ ] Graceful failure handling

## Success Criteria

**All tests must pass**:
- [ ] Organizer endpoints return all translations without Accept-Language header
- [ ] Public endpoints unchanged and still require Accept-Language header  
- [ ] Authentication and authorization working correctly
- [ ] Performance within constitutional limits (<2s response time)
- [ ] Error handling graceful and informative
- [ ] Data consistency between packs and options endpoints
- [ ] Backward compatibility maintained

**Ready for Production**:
- [ ] All quickstart tests passing
- [ ] Integration test suite passing
- [ ] OpenAPI documentation updated
- [ ] Frontend types updated (if needed)

## Troubleshooting

### Common Issues

**Issue**: 500 error on organizer endpoints  
**Solution**: Check that new repository methods are properly implemented and injected

**Issue**: Empty translations maps for all options  
**Solution**: Verify OptionTranslationsTable has data and new query logic is correct

**Issue**: Public endpoints broken  
**Solution**: Ensure existing repository methods and routes are unchanged

**Issue**: Authentication failures  
**Solution**: Verify AuthorizedOrganisationPlugin still properly configured on organizer routes

### Debug Information

**Check server logs for**:
- Correlation IDs for request tracing
- Database query logs for translation loading
- Authentication/authorization decision logs
- Error stack traces with context

**Database verification**:
```sql
-- Check translation data exists
SELECT * FROM option_translations WHERE option_id = 'your-option-id';

-- Verify pack-option relationships
SELECT * FROM pack_options WHERE pack_id = 'your-pack-id';
```

This quickstart validates that the feature works correctly for organizers while maintaining backward compatibility with existing public APIs.