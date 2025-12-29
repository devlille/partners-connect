# Quickstart: Job Offer Promotion Testing

## Overview
This quickstart guide provides step-by-step instructions to manually test the job offer promotion workflow from initial setup through approval/decline. Use this to validate the implementation meets all functional requirements.

---

## Prerequisites

### Running Services
- Backend server: `cd server && ./gradlew run --no-daemon`
- PostgreSQL: Running on localhost:5432 (or H2 for integration tests)
- Mailjet integration: Configured with valid API key (or mock for testing)
- Slack integration: Configured with valid webhook URL (or mock for testing)

### Test Data Setup
Run the integration test setup or use the following minimal data:

```sql
-- Organization
INSERT INTO organisations (id, name, slug, head_office, owner_email, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Test Org', 'test-org', '123 Main St', 'owner@test.com', NOW(), NOW());

-- Event (not ended)
INSERT INTO events (id, organisation_id, name, slug, start_time, end_time, submission_start_time, submission_end_time, contact_email, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'DevConf 2025', 'devconf-2025', '2025-11-01 09:00:00', '2025-11-03 18:00:00', '2025-10-01 00:00:00', '2025-10-31 23:59:59', 'contact@devconf.com', NOW(), NOW());

-- Company
INSERT INTO companies (id, name, owner_id, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440003', 'TechCorp', '550e8400-e29b-41d4-a716-446655440010', NOW(), NOW());

-- Partnership (active)
INSERT INTO partnerships (id, company_id, event_id, phone, language, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', '+33612345678', 'en', NOW(), NOW());

-- Partnership contacts
INSERT INTO partnership_emails (id, partnership_id, email, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440004', 'contact@techcorp.com', NOW(), NOW());

-- Job Offer
INSERT INTO company_job_offers (id, company_id, url, title, location, publication_date, end_date, experience_years, salary, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440003', 'https://techcorp.com/jobs/backend-dev', 'Senior Backend Developer', 'Remote', '2025-10-15 00:00:00', '2025-11-15 23:59:59', 5, '70k-90k EUR', NOW(), NOW());

-- User with event edit permission
INSERT INTO users (id, email, display_name, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440010', 'organizer@devconf.com', 'Event Organizer', NOW(), NOW());

INSERT INTO organisation_permissions (id, organisation_id, user_id, can_edit, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440010', true, NOW(), NOW());
```

---

## Test Scenarios

### Scenario 1: Successful Job Offer Promotion (FR-001 to FR-005, FR-013, FR-014)

**Objective**: Company owner promotes a job offer to an active partnership.

**Steps**:

1. **Authenticate as company owner**:
   ```bash
   JWT_TOKEN=$(curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "owner@techcorp.com", "password": "test123"}' \
     | jq -r '.token')
   ```

2. **Promote job offer**:
   ```bash
   curl -X POST http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/partnerships/550e8400-e29b-41d4-a716-446655440004/promote \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "job_offer_id": "550e8400-e29b-41d4-a716-446655440006"
     }'
   ```

**Expected Response** (201 Created):
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440000"
}
```

**Validation**:
- ✅ Response status 201
- ✅ Promotion ID returned
- ✅ Database record created with status=pending
- ✅ Mailjet email sent to contact@techcorp.com (check logs or mock)
- ✅ Slack notification sent to organization channel (check logs or mock)

---

### Scenario 2: Promotion After Event Ended (FR-030)

**Objective**: System rejects promotion attempts after event end date.

**Steps**:

1. **Create expired event** (or update existing event end_time to past):
   ```sql
   UPDATE events 
   SET end_time = '2025-10-01 00:00:00' 
   WHERE id = '550e8400-e29b-41d4-a716-446655440002';
   ```

2. **Attempt promotion**:
   ```bash
   curl -X POST http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/partnerships/550e8400-e29b-41d4-a716-446655440004/promote \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "job_offer_id": "550e8400-e29b-41d4-a716-446655440006"
     }'
   ```

**Expected Response** (403 Forbidden):
```json
{
  "error": "forbidden",
  "message": "Cannot promote job offers after event has ended"
}
```

**Validation**:
- ✅ Response status 403
- ✅ Error message clear and actionable
- ✅ No promotion record created
- ✅ No notifications sent

**Cleanup**: Restore event end_time to future date for subsequent tests.

---

### Scenario 3: Event Organizer Approves Promotion (FR-006, FR-008, FR-009, FR-011, FR-012, FR-015, FR-016)

**Objective**: User with event edit permission approves a pending promotion.

**Steps**:

1. **Authenticate as event organizer**:
   ```bash
   ORGANIZER_TOKEN=$(curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "organizer@devconf.com", "password": "test123"}' \
     | jq -r '.token')
   ```

2. **List pending promotions** (optional verification):
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/devconf-2025/job-offers?status=pending" \
     -H "Authorization: Bearer $ORGANIZER_TOKEN"
   ```

3. **Approve promotion**:
   ```bash
   curl -X POST http://localhost:8080/orgs/test-org/events/devconf-2025/partnerships/550e8400-e29b-41d4-a716-446655440004/job-offers/750e8400-e29b-41d4-a716-446655440000/approve \
     -H "Authorization: Bearer $ORGANIZER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{}'
   ```

**Expected Response** (200 OK):
```json
{
  "id": "750e8400-e29b-41d4-a716-446655440000",
  "job_offer_id": "550e8400-e29b-41d4-a716-446655440006",
  "partnership_id": "550e8400-e29b-41d4-a716-446655440004",
  "event_id": "550e8400-e29b-41d4-a716-446655440002",
  "status": "approved",
  "promoted_at": "2025-10-18T10:00:00Z",
  "reviewed_at": "2025-10-18T15:45:00Z",
  "reviewed_by": "550e8400-e29b-41d4-a716-446655440010",
  "job_offer": {
    "id": "550e8400-e29b-41d4-a716-446655440006",
    "title": "Senior Backend Developer",
    ...
  },
  "created_at": "2025-10-18T10:00:00Z",
  "updated_at": "2025-10-18T15:45:00Z"
}
```

**Validation**:
- ✅ Response status 200
- ✅ status = "approved"
- ✅ reviewed_at timestamp present
- ✅ reviewed_by = organizer user ID
- ✅ Mailjet approval email sent to partnership contacts
- ✅ Slack approval notification sent to organization channel

---

### Scenario 4: Event Organizer Declines Promotion (FR-007, FR-010, FR-017, FR-018)

**Objective**: Organizer declines a pending promotion with optional reason.

**Setup**: Create another promotion or re-promote the declined one.

**Steps**:

1. **Decline promotion with reason**:
   ```bash
   curl -X POST http://localhost:8080/orgs/test-org/events/devconf-2025/partnerships/550e8400-e29b-41d4-a716-446655440004/job-offers/850e8400-e29b-41d4-a716-446655440000/decline \
     -H "Authorization: Bearer $ORGANIZER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "reason": "Job offer not aligned with event audience"
     }'
   ```

**Expected Response** (200 OK):
```json
{
  "id": "850e8400-e29b-41d4-a716-446655440000",
  "status": "declined",
  "reviewed_at": "2025-10-18T16:00:00Z",
  "reviewed_by": "550e8400-e29b-41d4-a716-446655440010",
  ...
}
```

**Validation**:
- ✅ Response status 200
- ✅ status = "declined"
- ✅ reviewed_at and reviewed_by populated
- ✅ Mailjet decline email sent with reason included
- ✅ Slack decline notification sent

---

### Scenario 5: Re-Promotion of Declined Offer (FR-031)

**Objective**: Company can re-promote a declined job offer, resetting status to pending.

**Steps**:

1. **Re-promote the declined job offer** (same as Scenario 1):
   ```bash
   curl -X POST http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/partnerships/550e8400-e29b-41d4-a716-446655440004/promote \
     -H "Authorization: Bearer $JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "job_offer_id": "550e8400-e29b-41d4-a716-446655440006"
     }'
   ```

**Expected Response** (201 Created):
```json
{
  "id": "850e8400-e29b-41d4-a716-446655440000"
}
```

**Validation**:
- ✅ Same promotion ID returned (upsert, not duplicate)
- ✅ status reset to "pending"
- ✅ promoted_at updated to new timestamp
- ✅ reviewed_at and reviewed_by cleared to NULL
- ✅ New promotion notification sent

---

### Scenario 6: Unauthorized Access (FR-008)

**Objective**: User without event edit permission cannot approve/decline.

**Steps**:

1. **Authenticate as user without permission**:
   ```bash
   NO_PERM_TOKEN=$(curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "random@user.com", "password": "test123"}' \
     | jq -r '.token')
   ```

2. **Attempt approval**:
   ```bash
   curl -X POST http://localhost:8080/orgs/test-org/events/devconf-2025/partnerships/550e8400-e29b-41d4-a716-446655440004/job-offers/850e8400-e29b-41d4-a716-446655440000/approve \
     -H "Authorization: Bearer $NO_PERM_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{}'
   ```

**Expected Response** (403 Forbidden):
```json
{
  "error": "forbidden",
  "message": "User does not have edit permission for this organization"
}
```

**Validation**:
- ✅ Response status 403
- ✅ No status change in database
- ✅ No notifications sent

---

### Scenario 7: Cascade Delete on Job Offer Deletion (FR-024)

**Objective**: Promotions are deleted when source job offer is deleted.

**Steps**:

1. **Create promotion** (as in Scenario 1)

2. **Verify promotion exists**:
   ```bash
   curl -X GET http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/job-offers/550e8400-e29b-41d4-a716-446655440006/promotions \
     -H "Authorization: Bearer $JWT_TOKEN"
   ```

3. **Delete job offer**:
   ```bash
   curl -X DELETE http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/job-offers/550e8400-e29b-41d4-a716-446655440006 \
     -H "Authorization: Bearer $JWT_TOKEN"
   ```

4. **Verify promotion deleted**:
   ```sql
   SELECT * FROM company_job_offer_promotions 
   WHERE job_offer_id = '550e8400-e29b-41d4-a716-446655440006';
   ```

**Expected Result**: Zero rows (cascade delete)

**Validation**:
- ✅ Job offer deleted (204 No Content)
- ✅ Associated promotions automatically deleted
- ✅ No orphaned promotion records

---

### Scenario 8: Partnership Termination Preserves Promotions (FR-032)

**Objective**: Promotions remain in pending state when partnership is deleted/terminated.

**Steps**:

1. **Create promotion in pending state**

2. **Delete partnership**:
   ```sql
   DELETE FROM partnerships WHERE id = '550e8400-e29b-41d4-a716-446655440004';
   ```

3. **Verify promotion still exists**:
   ```sql
   SELECT * FROM company_job_offer_promotions 
   WHERE partnership_id = '550e8400-e29b-41d4-a716-446655440004';
   ```

**Expected Result**: Promotion record still exists with status unchanged.

**Validation**:
- ✅ Partnership deleted
- ✅ Promotion NOT deleted (ON DELETE NO ACTION)
- ✅ status remains "pending"

---

### Scenario 9: Multiple Language Notifications (FR-020)

**Objective**: Notifications use partnership language preference.

**Steps**:

1. **Create French partnership**:
   ```sql
   UPDATE partnerships 
   SET language = 'fr' 
   WHERE id = '550e8400-e29b-41d4-a716-446655440004';
   ```

2. **Promote job offer** (as in Scenario 1)

3. **Check notification logs/mocks**:
   - Mailjet should use `content.fr.html` and `header.fr.txt`
   - Slack should use `fr.md` notification template

**Validation**:
- ✅ French email template loaded
- ✅ French Slack message sent
- ✅ Variables correctly populated in French content

---

### Scenario 10: List and Filter Promotions (FR-026, FR-027, FR-029)

**Objective**: Organizers can query promotions by event and filter by status.

**Steps**:

1. **List all event promotions**:
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/devconf-2025/job-offers" \
     -H "Authorization: Bearer $ORGANIZER_TOKEN"
   ```

2. **Filter by pending status**:
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/devconf-2025/job-offers?status=pending" \
     -H "Authorization: Bearer $ORGANIZER_TOKEN"
   ```

3. **Filter by approved status**:
   ```bash
   curl -X GET "http://localhost:8080/orgs/test-org/events/devconf-2025/job-offers?status=approved" \
     -H "Authorization: Bearer $ORGANIZER_TOKEN"
   ```

**Validation**:
- ✅ All endpoints return 200 OK
- ✅ Pagination metadata correct (total, page, total_pages)
- ✅ Filtering accurately reflects status
- ✅ Only promotions for specified event returned

---

## Integration Test Execution

Run the full integration test suite:

```bash
cd server
./gradlew test --tests "*JobOfferPromotion*" --no-daemon
```

**Expected**: All tests pass with >80% coverage.

---

## Notification Verification

### Mock Setup (for testing without external services)

Create mock notification gateways in test configuration:

```kotlin
// In test setup
val mockMailjetGateway = object : NotificationGateway {
    override val provider = IntegrationProvider.MAILJET
    override fun send(integrationId: UUID, variables: NotificationVariables): Boolean {
        println("MOCK MAILJET: ${variables.usageName} to ${variables.company.name}")
        return true
    }
}

val mockSlackGateway = object : NotificationGateway {
    override val provider = IntegrationProvider.SLACK
    override fun send(integrationId: UUID, variables: NotificationVariables): Boolean {
        println("MOCK SLACK: ${variables.usageName} for event ${variables.event.event.name}")
        return true
    }
}
```

### Production Verification

Check actual notification delivery:

1. **Mailjet**: Login to Mailjet dashboard → Message History → search for job offer promotion emails
2. **Slack**: Check configured organization channel for notification messages
3. **Logs**: Search application logs for "Notification sent" entries

---

## Performance Validation

**Requirement**: All endpoints must respond within 2 seconds.

**Load Test** (using Apache Bench or similar):
```bash
ab -n 100 -c 10 -H "Authorization: Bearer $JWT_TOKEN" \
  -p promote_payload.json -T application/json \
  http://localhost:8080/companies/550e8400-e29b-41d4-a716-446655440003/partnerships/550e8400-e29b-41d4-a716-446655440004/promote
```

**Expected**: p95 latency < 2000ms

---

## Rollback Test

**Objective**: Verify feature can be safely rolled back if issues detected.

**Steps**:

1. **Drop promotion table**:
   ```sql
   DROP TABLE IF EXISTS company_job_offer_promotions CASCADE;
   ```

2. **Restart application**

3. **Verify existing endpoints still work**:
   - GET /companies should return 200
   - POST /companies/{id}/job-offers should return 201
   - Partnership endpoints should return 200

**Validation**: Application runs without errors, pre-existing features unaffected.

---

## Checklist

Use this checklist to verify all functional requirements:

- [ ] FR-001: Company can promote job offers
- [ ] FR-002: Partnership validation enforced
- [ ] FR-003: Duplicate prevention (via upsert)
- [ ] FR-004: Initial status is pending
- [ ] FR-005: Promotions linked to job offer, partnership, event
- [ ] FR-006: Organizer can approve
- [ ] FR-007: Organizer can decline
- [ ] FR-008: Permission validation (canEdit)
- [ ] FR-009: Approved status set correctly
- [ ] FR-010: Declined status set correctly
- [ ] FR-011: Reviewed timestamp recorded
- [ ] FR-012: Reviewer user ID recorded
- [ ] FR-013: Mailjet email on promotion
- [ ] FR-014: Slack notification on promotion
- [ ] FR-015: Mailjet email on approval
- [ ] FR-016: Slack notification on approval
- [ ] FR-017: Mailjet email on decline
- [ ] FR-018: Slack notification on decline
- [ ] FR-019: Notification details include required fields
- [ ] FR-020: Multi-language support
- [ ] FR-021: Notification failures logged, don't block
- [ ] FR-022: Job offer foreign key enforced
- [ ] FR-023: Partnership foreign key enforced
- [ ] FR-024: Cascade delete on job offer
- [ ] FR-025: Promotion history persisted
- [ ] FR-026: List promotions by event
- [ ] FR-027: Filter by status
- [ ] FR-028: List promotions by job offer
- [ ] FR-029: Query pending promotions
- [ ] FR-030: Reject promotion after event ends
- [ ] FR-031: Allow re-promotion of declined
- [ ] FR-032: Preserve promotions on partnership termination

---

*Quickstart complete. All test scenarios defined with expected results. Ready for implementation validation.*
