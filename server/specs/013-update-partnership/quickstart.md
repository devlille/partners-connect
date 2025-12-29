# Quickstart Guide: Testing Partnership Contact Updates

**Feature**: Update Partnership Contact Information  
**Branch**: 013-update-partnership  
**Date**: 2025-11-29

## Prerequisites

- PostgreSQL database running (or H2 for tests)
- Server running on port 8080
- Existing event with slug (e.g., `devlille-2025`)
- Existing partnership with UUID

## Quick Test Commands

### 1. Update All Contact Fields

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "contact_name": "John Doe",
    "contact_role": "Developer Relations Manager",
    "language": "en",
    "phone": "+33123456789",
    "emails": ["john.doe@example.com", "contact@example.com"]
  }'
```

**Expected**: 200 OK with complete partnership object including updated contact info

### 2. Partial Update (Name Only)

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "contact_name": "Jane Smith"
  }'
```

**Expected**: 200 OK, only `contact_name` changed, other fields unchanged

### 3. Update Language and Phone

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "language": "fr",
    "phone": "+33612345678"
  }'
```

**Expected**: 200 OK with French language and new phone number

### 4. Clear Phone Number

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "phone": null
  }'
```

**Expected**: 200 OK with phone field set to null

### 5. Update Emails Only

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "emails": ["new-contact@example.com", "support@example.com"]
  }'
```

**Expected**: 200 OK with updated emails array

## Error Cases to Test

### Invalid Email Format

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "emails": ["invalid-email"]
  }'
```

**Expected**: 400 Bad Request with validation error message

### Invalid Language Code

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "language": "invalid"
  }'
```

**Expected**: 400 Bad Request with enum validation error

### Phone Too Long

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "12345678901234567890123456789012345"
  }'
```

**Expected**: 400 Bad Request with maxLength validation error

### Non-Existent Partnership

```bash
curl -X PUT http://localhost:8080/events/devlille-2025/partnerships/00000000-0000-0000-0000-000000000000 \
  -H "Content-Type: application/json" \
  -d '{
    "contact_name": "Test"
  }'
```

**Expected**: 404 Not Found

### Non-Existent Event

```bash
curl -X PUT http://localhost:8080/events/invalid-event/partnerships/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "contact_name": "Test"
  }'
```

**Expected**: 404 Not Found

## Running Tests

### Contract Tests

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect/server
./gradlew test --tests "PartnershipContactInfoUpdateContractTest" --no-daemon
```

**Tests include**:
- ✅ Valid request with all fields passes schema validation
- ✅ Partial update with single field passes validation
- ✅ Invalid email format fails validation
- ✅ Invalid language code fails validation
- ✅ Phone exceeding 30 chars fails validation

### Integration Tests

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect/server
./gradlew test --tests "PartnershipContactInfoUpdateIntegrationTest" --no-daemon
```

**Tests include**:
- ✅ Update all fields returns 200 with updated data
- ✅ Partial update modifies only specified fields
- ✅ Update non-existent partnership returns 404
- ✅ Update with invalid event slug returns 404
- ✅ Concurrent updates don't corrupt data
- ✅ Empty request body returns 200 (no-op)

### Full Test Suite

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect/server
./gradlew test --no-daemon
```

## Validation Checklist

Before merging:

- [ ] OpenAPI spec validates: `npm run validate`
- [ ] ktlint passes: `./gradlew ktlintCheck --no-daemon`
- [ ] detekt passes: `./gradlew detekt --no-daemon`
- [ ] Contract tests pass: all schema validation scenarios
- [ ] Integration tests pass: all HTTP endpoint scenarios
- [ ] Manual testing: all quickstart curl commands work
- [ ] Edge cases: 404 errors, validation errors return proper messages
- [ ] Documentation: OpenAPI spec includes PATCH operation
- [ ] Schema file: copied to `server/application/src/main/resources/schemas/`

## Development Workflow

1. **Setup Database**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect/server
   docker-compose up -d postgres
   ```

2. **Run Server**:
   ```bash
   ./gradlew run --no-daemon
   ```

3. **Create Test Data** (using existing endpoints):
   ```bash
   # Create event (if needed - usually via org admin)
   # Create company
   # Create partnership
   ```

4. **Test Update Endpoint**:
   - Use curl commands above
   - Verify responses match expected behavior
   - Check database to confirm persistence

5. **Run Tests**:
   ```bash
   ./gradlew test --no-daemon
   ```

## Expected Response Format

```json
{
  "partnership": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "contact_name": "John Doe",
    "contact_role": "Developer Relations Manager",
    "language": "en",
    "phone": "+33123456789",
    "emails": ["john.doe@example.com", "contact@example.com"],
    "selected_pack": { ... },
    "validated_at": "2025-11-29T10:00:00Z",
    "created_at": "2025-11-01T08:00:00Z"
  },
  "company": { ... },
  "event": { ... },
  "organisation": { ... },
  "speakers": []
}
```

## Troubleshooting

### 500 Internal Server Error
- Check server logs for stack trace
- Verify database migration ran successfully
- Confirm `emails` column exists in `partnerships` table

### 400 Bad Request without clear message
- Check request JSON is valid
- Verify Content-Type header is `application/json`
- Review schema validation errors in server logs

### 404 Not Found
- Verify event slug is correct
- Confirm partnership ID exists in database
- Check partnership belongs to specified event

### Empty response body
- Verify Accept header if set
- Check server serialization configuration
- Review StatusPages exception handling

## Performance Benchmarks

Expected performance targets (per success criteria):

- **Response Time**: <2 seconds for validation errors (SC-003)
- **Update Time**: <30 seconds total for complete update flow (SC-001)
- **Database Query**: <100ms for partnership lookup and update
- **Throughput**: Should handle 10+ concurrent updates without degradation

## Next Steps

After quickstart validation:

1. Run full test suite
2. Verify OpenAPI documentation renders correctly
3. Test with Bruno/Postman collections
4. Update frontend integration (if applicable)
5. Monitor production metrics after deployment
