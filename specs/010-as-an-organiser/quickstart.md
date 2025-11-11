# Quickstart Guide: OpenPlanner Speaker-Partnership Integration

## Prerequisites

1. **Development Environment**:
   ```bash
   # Ensure Java 21 and Gradle 8+ installed
   java --version  # Should show 21+
   ./gradlew --version  # Should show 8.13+
   ```

2. **Database Setup**:
   ```bash
   # Start PostgreSQL via Docker Compose
   cd server
   docker-compose up -d postgres
   ```

3. **Test Environment**:
   ```bash
   # Verify tests run (uses H2 in-memory)
   cd server
   ./gradlew test --no-daemon
   ```

## Feature Validation Workflow

### 1. Import Agenda from OpenPlanner

**Setup**: Create test event with OpenPlanner integration configured

**Test Request**:
```bash
# Import agenda for event (requires organization permission)
curl -X POST "http://localhost:8080/orgs/devlille/events/devlille-2025/agenda" \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json"
```

**Expected Responses**:
- **201 Created**: Agenda imported, sessions and speakers created/updated
- **503 Service Unavailable**: OpenPlanner API unavailable (no database changes)
- **404 Not Found**: Event not found or no integration configured
- **401 Unauthorized**: Invalid JWT token

**Verification**:
```bash
# Check that agenda import was successful by retrieving the imported data
curl "http://localhost:8080/orgs/devlille/events/devlille-2025/agenda" \
  -H "Authorization: Bearer {jwt_token}"

# Expected response includes sessions and speakers arrays
```

### 1.1. Retrieve Imported Agenda

**Test Request**:
```bash
# Get imported agenda data (sessions and speakers)
curl "http://localhost:8080/orgs/devlille/events/devlille-2025/agenda" \
  -H "Authorization: Bearer {jwt_token}"
```

**Expected Response** (200 OK):
```json
{
  "sessions": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "name": "Introduction to Kotlin",
      "abstract": "Getting started with Kotlin programming",
      "start_time": "2025-11-11T10:00:00Z",
      "end_time": "2025-11-11T11:00:00Z",
      "track_name": "Development Track",
      "language": "en"
    }
  ],
  "speakers": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "name": "Jane Doe",
      "biography": "Kotlin expert with 5 years experience",
      "job_title": "Senior Developer",
      "photo_url": "https://example.com/photo.jpg",
      "pronouns": "she/her"
    }
  ]
}
```

### 2. Attach Speaker to Partnership

**Setup**: Ensure approved partnership exists for event

**Test Request**:
```bash
# Attach speaker to partnership (public endpoint, no auth required)
PARTNERSHIP_ID="550e8400-e29b-41d4-a716-446655440000"
SPEAKER_ID="650e8400-e29b-41d4-a716-446655440000" 

curl -X POST "http://localhost:8080/partnerships/${PARTNERSHIP_ID}/speakers/${SPEAKER_ID}" \
  -H "Content-Type: application/json" \
  -d "{}"
```

**Expected Response** (201 Created):
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440000",
  "speaker_id": "650e8400-e29b-41d4-a716-446655440000", 
  "partnership_id": "550e8400-e29b-41d4-a716-446655440000",
  "created_at": "2025-11-11T14:30:00Z"
}
```

**Error Cases**:
```bash
# 409 Conflict: Speaker already attached
# 403 Forbidden: Partnership not approved
# 404 Not Found: Speaker or partnership not found
# 400 Bad Request: Speaker not from partnership's event
```

### 3. View Partnership with Speakers

**Test Request**:
```bash
# Get partnership details (includes attached speakers automatically)
curl "http://localhost:8080/partnerships/${PARTNERSHIP_ID}"
```

**Expected Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "contact_name": "John Smith",
  "contact_role": "Marketing Manager",
  "language": "en",
  "emails": ["john.smith@acme.com"],
  "process_status": {
    "suggestion_sent_at": null,
    "suggestion_approved_at": "2025-11-09T10:00:00Z",
    "suggestion_declined_at": null,
    "validated_at": "2025-11-09T15:30:00Z",
    "declined_at": null,
    "agreement_url": "https://example.com/agreement.pdf",
    "agreement_signed_url": null,
    "communication_publication_date": null,
    "communication_support_url": null,
    "billing_status": null
  },
  "speakers": [
    {
      "id": "650e8400-e29b-41d4-a716-446655440000",
      "name": "Jane Doe",
      "biography": "Software engineer and tech speaker", 
      "job_title": "Senior Developer",
      "photo_url": "https://example.com/photo.jpg",
      "pronouns": "she/her"
    }
  ],
  "created_at": "2025-11-10T09:00:00Z"
}
```

### 4. Remove Speaker from Partnership

**Test Request**:
```bash  
# Detach speaker from partnership
curl -X DELETE "http://localhost:8080/partnerships/${PARTNERSHIP_ID}/speakers/${SPEAKER_ID}"
```

**Expected Response**: 204 No Content

## Integration Test Scenarios

### Test Scenario 1: Complete Import and Attachment Flow
```bash
# 1. Import agenda (creates speakers and sessions)
# 2. Retrieve agenda to verify import success and get speaker IDs
# 3. Create approved partnership  
# 4. Attach imported speaker to partnership using speaker ID from agenda
# 5. Verify partnership details include attached speakers automatically
# 6. Test speaker detachment
```

### Test Scenario 2: Error Handling
```bash
# 1. Test OpenPlanner API failure during import
# 2. Verify database remains unchanged
# 3. Test attachment to unapproved partnership  
# 4. Test duplicate speaker attachment
# 5. Test cross-event speaker attachment (should fail)
```

### Test Scenario 3: Authorization Boundaries  
```bash
# 1. Test agenda import without organization permission
# 2. Verify public speaker attachment works without auth
# 3. Test partnership detail access (public endpoint)
```

## Database State Verification

### Check Created Tables
```sql
-- Verify speaker-partnership associations
SELECT sp.id, s.name, c.name as company 
FROM speaker_partnerships sp
JOIN speakers s ON sp.speaker_id = s.id  
JOIN partnerships p ON sp.partnership_id = p.id
JOIN companies c ON p.company_id = c.id;

-- Verify speakers have OpenPlanner external IDs
SELECT id, name, external_id 
FROM speakers 
WHERE external_id IS NOT NULL;
```

### Verify Constraints
```sql
-- Test unique constraint
INSERT INTO speaker_partnerships (speaker_id, partnership_id) 
VALUES (same_speaker, same_partnership); -- Should fail

-- Test foreign key constraints  
INSERT INTO speaker_partnerships (speaker_id, partnership_id)
VALUES ('non-existent-id', 'non-existent-id'); -- Should fail
```

## Troubleshooting

### Common Issues
1. **OpenPlanner API Key**: Verify integration configuration has valid API key
2. **Partnership Status**: Ensure partnership status is APPROVED before attachment
3. **Event Mismatch**: Verify speaker belongs to same event as partnership
4. **Authorization**: Check JWT token validity and organization permissions

### Debug Commands
```bash
# Check server logs for detailed error messages
docker-compose logs -f partners-connect-server

# Verify database state
docker-compose exec postgres psql -U partners -d partners_connect

# Run specific test suite  
./gradlew test --tests "*PartnershipSpeaker*" --no-daemon
```