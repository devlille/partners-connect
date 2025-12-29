# Quickstart: Manual Testing Guide for Partnership Email Feature

**Feature**: 014-email-partnerships  
**Version**: 1.0  
**Purpose**: Step-by-step guide for manually testing the email partnership feature

---

## Prerequisites

### 1. Server Setup

**Required**:
- PostgreSQL database running with test data
- Server running on `http://localhost:8080`
- Valid JWT token with edit permissions for test organization
- Mailjet integration configured for test organization

**Architecture Note**: This endpoint uses a 3-repository pattern:
- `PartnershipEmailRepository` - Fetches partnerships with email data
- `EventRepository` - Fetches event details for email context
- `NotificationRepository` - Sends emails via Mailjet gateway (internally uses `IntegrationRepository` for config lookup)

**Start Server**:
```bash
cd server
./gradlew run --no-daemon
```

**Verify Server Running**:
```bash
curl http://localhost:8080/health
# Expected: {"status": "UP"}
```

---

### 2. Database Test Data

**Required Records**:
1. **Organization**: `devlille` (slug)
2. **Event**: `devlille-2025` (slug) under `devlille` organization
3. **Mailjet Integration**: Configured for `devlille` organization
4. **User**: Test user with edit permission for `devlille`
5. **Partnerships**: At least 3 partnerships for `devlille-2025` event:
   - Partnership 1: Assigned to organizer User A, 2 contact emails
   - Partnership 2: Assigned to organizer User B, 1 contact email
   - Partnership 3: No organizer assigned, 1 contact email

**Setup Script** (SQL):
```sql
-- Insert test organization
INSERT INTO organisations (id, slug, name) 
VALUES ('00000000-0000-0000-0000-000000000001', 'devlille', 'DevLille Community');

-- Insert test event
INSERT INTO events (id, slug, name, contact_email, organisation_id) 
VALUES (
  '00000000-0000-0000-0000-000000000002', 
  'devlille-2025', 
  'DevLille 2025', 
  'event@devlille.com',
  '00000000-0000-0000-0000-000000000001'
);

-- Insert test users (organizers)
INSERT INTO users (id, email, firstname, lastname) 
VALUES 
  ('00000000-0000-0000-0000-000000000003', 'alice@example.com', 'Alice', 'Smith'),
  ('00000000-0000-0000-0000-000000000004', 'bob@example.com', 'Bob', 'Jones');

-- Insert test companies
INSERT INTO companies (id, name) 
VALUES 
  ('00000000-0000-0000-0000-000000000010', 'Company A'),
  ('00000000-0000-0000-0000-000000000011', 'Company B'),
  ('00000000-0000-0000-0000-000000000012', 'Company C');

-- Insert test sponsoring pack
INSERT INTO sponsoring_packs (id, name, event_id, price) 
VALUES (
  '00000000-0000-0000-0000-000000000020',
  'Gold Pack',
  '00000000-0000-0000-0000-000000000002',
  5000
);

-- Insert test partnerships
INSERT INTO partnerships (id, company_id, event_id, organiser_user_id, validated_pack_id) 
VALUES 
  -- Partnership 1: Alice, validated
  (
    '00000000-0000-0000-0000-000000000030',
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000020'
  ),
  -- Partnership 2: Bob, validated
  (
    '00000000-0000-0000-0000-000000000031',
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000020'
  ),
  -- Partnership 3: No organizer, validated
  (
    '00000000-0000-0000-0000-000000000032',
    '00000000-0000-0000-0000-000000000012',
    '00000000-0000-0000-0000-000000000002',
    NULL,
    '00000000-0000-0000-0000-000000000020'
  );

-- Insert contact emails for partnerships
INSERT INTO company_emails (id, partnership_id, email) 
VALUES 
  -- Partnership 1 (Alice): 2 emails
  ('00000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000000030', 'contact1@companyA.com'),
  ('00000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000030', 'contact2@companyA.com'),
  -- Partnership 2 (Bob): 1 email
  ('00000000-0000-0000-0000-000000000042', '00000000-0000-0000-0000-000000000031', 'contact3@companyB.com'),
  -- Partnership 3 (No organizer): 1 email
  ('00000000-0000-0000-0000-000000000043', '00000000-0000-0000-0000-000000000032', 'contact4@companyC.com');

-- Insert Mailjet integration (replace with real API key/secret for actual testing)
INSERT INTO integrations (id, organisation_id, type) 
VALUES (
  '00000000-0000-0000-0000-000000000050',
  '00000000-0000-0000-0000-000000000001',
  'MAILJET'
);

INSERT INTO mailjet_integrations (integration_id, api_key, secret) 
VALUES (
  '00000000-0000-0000-0000-000000000050',
  'YOUR_MAILJET_API_KEY',
  'YOUR_MAILJET_SECRET'
);
```

---

### 3. JWT Token Setup

**Option A: Obtain token via OAuth flow**
```bash
# Authenticate via Google OAuth (browser)
open http://localhost:8080/auth/google

# After authentication, extract JWT from cookie or response
```

**Option B: Generate test token (for local testing only)**
```bash
# Use test token from server configuration
export JWT_TOKEN="your-test-jwt-token-here"
```

**Verify Token**:
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/users/me
# Expected: User details JSON
```

---

## Test Scenarios

### Test 1: Send Email to All Validated Partnerships

**Objective**: Verify email sent successfully to all partnerships with validated packs

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Welcome to DevLille 2025",
    "body": "<h1>Thank you for your partnership!</h1><p>We are excited to have you as a sponsor for DevLille 2025.</p><ul><li>Event date: June 15-16, 2025</li><li>Venue: Lille Grand Palais</li></ul>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 4
}
```

**Explanation**:
- Partnership 1 (Alice): 2 emails (contact1, contact2)
- Partnership 2 (Bob): 1 email (contact3)
- Partnership 3 (No organizer): 1 email (contact4)
- **Total: 4 unique recipients**

**Mailjet Batches Sent**:
1. **Batch 1** (From: alice@example.com, CC: event@devlille.com, To: [contact1, contact2])
2. **Batch 2** (From: bob@example.com, CC: event@devlille.com, To: [contact3])
3. **Batch 3** (From: event@devlille.com, To: [contact4])

**Verification**:
- Check Mailjet dashboard for 3 API calls
- Verify each recipient received email with correct sender
- Confirm subject line has `[DevLille 2025]` prefix

---

### Test 2: Send Email with HTML Content

**Objective**: Verify rich HTML content is sent correctly

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Booth Setup Instructions",
    "body": "<html><body><h1 style=\"color: blue;\">Booth Setup Details</h1><table border=\"1\"><tr><th>Time</th><th>Activity</th></tr><tr><td>9:00 AM</td><td>Setup begins</td></tr><tr><td>5:00 PM</td><td>Event opens</td></tr></table><p><strong>Important:</strong> Bring your confirmation email.</p></body></html>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 4
}
```

**Verification**:
- Open received email in mail client
- Confirm HTML rendering: blue heading, table with border, bold text
- Verify subject: `[DevLille 2025] Booth Setup Instructions`

---

### Test 3: Filter by Specific Pack ID

**Objective**: Send email only to partnerships with specific validated pack

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true&filter%5Bpack_id%5D=00000000-0000-0000-0000-000000000020" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Gold Pack Exclusive Update",
    "body": "<p>As a Gold sponsor, you receive exclusive benefits...</p>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 4
}
```

**Explanation**: All 3 test partnerships have `validated_pack_id = Gold Pack UUID`

---

### Test 4: No Partnerships Match Filters

**Objective**: Verify 404 error when no partnerships match filters

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bpaid%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Test",
    "body": "<p>Test email</p>"
  }'
```

**Expected Response** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "No partnerships found matching the filters",
  "status": 404
}
```

**Explanation**: Test partnerships have no `billing_id` set (not paid)

---

### Test 5: Missing Subject Field (Validation Error)

**Objective**: Verify schema validation rejects missing subject

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "body": "<p>Email without subject</p>"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "error": "Bad Request",
  "message": "Validation failed: subject is required",
  "status": 400
}
```

---

### Test 6: Empty Subject Field (Validation Error)

**Objective**: Verify schema validation rejects empty subject

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "",
    "body": "<p>Email with empty subject</p>"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "error": "Bad Request",
  "message": "Validation failed: subject must not be empty",
  "status": 400
}
```

---

### Test 7: Subject Too Long (Validation Error)

**Objective**: Verify schema validation rejects subject > 500 characters

**Request**:
```bash
LONG_SUBJECT=$(python3 -c "print('A' * 501)")

curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d "{
    \"subject\": \"$LONG_SUBJECT\",
    \"body\": \"<p>Test</p>\"
  }"
```

**Expected Response** (400 Bad Request):
```json
{
  "error": "Bad Request",
  "message": "Validation failed: subject must not exceed 500 characters",
  "status": 400
}
```

---

### Test 8: Unauthorized User (No JWT Token)

**Objective**: Verify authentication required

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Test",
    "body": "<p>Test</p>"
  }'
```

**Expected Response** (401 Unauthorized):
```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid authentication token",
  "status": 401
}
```

---

### Test 9: User Without Edit Permission

**Objective**: Verify authorization enforces edit permission

**Setup**: Generate JWT token for user with read-only permission for `devlille` org

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $READ_ONLY_JWT_TOKEN" \
  -d '{
    "subject": "Test",
    "body": "<p>Test</p>"
  }'
```

**Expected Response** (401 Unauthorized):
```json
{
  "error": "Unauthorized",
  "message": "You do not have permission to edit this organization",
  "status": 401
}
```

---

### Test 10: Event Not Found

**Objective**: Verify 404 error for non-existent event

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/nonexistent-event/partnerships/email" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Test",
    "body": "<p>Test</p>"
  }'
```

**Expected Response** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "Event not found: nonexistent-event",
  "status": 404
}
```

---

### Test 11: Mailjet Integration Not Configured

**Objective**: Verify 404 error when Mailjet not configured

**Setup**: Remove Mailjet integration for organization

```sql
DELETE FROM mailjet_integrations WHERE integration_id = '00000000-0000-0000-0000-000000000050';
DELETE FROM integrations WHERE id = '00000000-0000-0000-0000-000000000050';
```

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Test",
    "body": "<p>Test</p>"
  }'
```

**Expected Response** (404 Not Found):
```json
{
  "error": "Not Found",
  "message": "Mailjet integration not configured for organisation",
  "status": 404
}
```

**Cleanup**: Re-insert Mailjet integration for subsequent tests

---

### Test 12: Mailjet API Failure (Simulated)

**Objective**: Verify 503 error when Mailjet API is unavailable

**Setup**: Temporarily set invalid Mailjet credentials

```sql
UPDATE mailjet_integrations 
SET api_key = 'invalid', secret = 'invalid' 
WHERE integration_id = '00000000-0000-0000-0000-000000000050';
```

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Test",
    "body": "<p>Test</p>"
  }'
```

**Expected Response** (503 Service Unavailable):
```json
{
  "error": "Service Unavailable",
  "message": "Email service is currently unavailable. Please try again later.",
  "status": 503
}
```

**Cleanup**: Restore valid Mailjet credentials

---

### Test 13: Email Deduplication

**Objective**: Verify duplicate contact emails receive only one email

**Setup**: Add duplicate email to second partnership

```sql
INSERT INTO company_emails (id, partnership_id, email) 
VALUES (
  '00000000-0000-0000-0000-000000000044',
  '00000000-0000-0000-0000-000000000030',
  'contact3@companyB.com'  -- Same as Partnership 2
);
```

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Deduplication Test",
    "body": "<p>Testing email deduplication</p>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 4
}
```

**Explanation**:
- Partnership 1 (Alice): 3 emails (contact1, contact2, contact3)
- Partnership 2 (Bob): 1 email (contact3) → **DUPLICATE, ignored in Bob's group**
- Partnership 3 (No organizer): 1 email (contact4)
- **Total: 4 unique recipients** (contact3 counted only once in Alice's group)

**Verification**:
- Check `contact3@companyB.com` inbox
- Confirm only **1 email received** (from Alice, not Bob, since Partnership 1 processed first)

**Cleanup**:
```sql
DELETE FROM company_emails WHERE id = '00000000-0000-0000-0000-000000000044';
```

---

### Test 14: Sort Direction (Ascending)

**Objective**: Verify partnerships can be sorted ascending by creation date

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true&direction=asc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Sort Test Ascending",
    "body": "<p>Testing ascending sort</p>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 4
}
```

**Note**: Sort direction affects query order but not response content or Mailjet batches.

---

### Test 15: Multiple Filters Combined

**Objective**: Verify multiple filters applied together (AND logic)

**Setup**: Mark Partnership 1 as paid

```sql
-- Create invoice
INSERT INTO invoices (id, partnership_id, status, amount) 
VALUES (
  '00000000-0000-0000-0000-000000000060',
  '00000000-0000-0000-0000-000000000030',
  'PAID',
  5000
);

-- Link invoice to partnership
UPDATE partnerships 
SET billing_id = '00000000-0000-0000-0000-000000000060' 
WHERE id = '00000000-0000-0000-0000-000000000030';
```

**Request**:
```bash
curl -X POST \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships/email?filter%5Bvalidated%5D=true&filter%5Bpaid%5D=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "subject": "Paid Partnerships Only",
    "body": "<p>Thank you for your payment!</p>"
  }'
```

**Expected Response** (200 OK):
```json
{
  "recipients": 2
}
```

**Explanation**: Only Partnership 1 matches both filters (validated AND paid)

**Cleanup**:
```sql
UPDATE partnerships SET billing_id = NULL WHERE id = '00000000-0000-0000-0000-000000000030';
DELETE FROM invoices WHERE id = '00000000-0000-0000-0000-000000000060';
```

---

## Mailjet Dashboard Verification

### View Sent Messages

1. **Login to Mailjet**:
   - URL: https://app.mailjet.com/
   - Use credentials for `YOUR_MAILJET_API_KEY`

2. **Navigate to Statistics**:
   - Dashboard → Statistics → Messages

3. **Filter by Campaign**:
   - Search for subject line (e.g., "Welcome to DevLille 2025")
   - Verify send time matches test execution

4. **Check Delivery Status**:
   - **Sent**: Mailjet accepted message
   - **Delivered**: Recipient mail server accepted message
   - **Opened**: Recipient opened email (if tracking enabled)

5. **Review Recipients**:
   - Click on message to view recipient list
   - Verify all expected email addresses present
   - Confirm no duplicates

---

## Common Issues & Troubleshooting

### Issue 1: 401 Unauthorized Despite Valid Token

**Symptoms**: Request returns 401 even with valid JWT

**Possible Causes**:
- User not member of organization
- User has read-only role (lacks edit permission)
- Token expired

**Debug Steps**:
```bash
# Verify token claims
echo $JWT_TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson'

# Check user permissions
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8080/orgs/devlille/users

# Verify user has edit role
```

**Solution**: Ensure user has edit permission for organization

---

### Issue 2: 404 "No partnerships found matching the filters"

**Symptoms**: Request returns 404 despite partnerships existing

**Possible Causes**:
- Partnerships don't match applied filters (e.g., filter[validated]=true but validated_pack_id is null)
- Event slug incorrect
- Partnerships belong to different event

**Debug Steps**:
```bash
# List all partnerships for event (via GET endpoint)
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8080/orgs/devlille/events/devlille-2025/partnerships?filter%5Bvalidated%5D=true"

# Check database directly
psql -d partners_connect -c "SELECT id, validated_pack_id, organiser_user_id FROM partnerships WHERE event_id = (SELECT id FROM events WHERE slug = 'devlille-2025');"
```

**Solution**: Verify partnerships have expected filter attributes set

---

### Issue 3: 503 Service Unavailable

**Symptoms**: Request returns 503 with "Email service is currently unavailable"

**Possible Causes**:
- Mailjet API down
- Invalid Mailjet credentials
- Network connectivity issues
- Mailjet quota exceeded

**Debug Steps**:
```bash
# Test Mailjet credentials directly
curl -X POST \
  https://api.mailjet.com/v3.1/send \
  -u "YOUR_API_KEY:YOUR_SECRET" \
  -H "Content-Type: application/json" \
  -d '{
    "Messages": [{
      "From": {"Email": "test@example.com"},
      "To": [{"Email": "recipient@example.com"}],
      "Subject": "Test",
      "TextPart": "Test"
    }]
  }'

# Check Mailjet status page
open https://status.mailjet.com/
```

**Solution**: 
- Wait if Mailjet experiencing outage
- Verify credentials in database
- Check Mailjet quota limits

---

### Issue 4: Emails Not Received by Recipients

**Symptoms**: Request returns 200 OK but recipients don't receive emails

**Possible Causes**:
- Emails in spam folder
- Invalid recipient email addresses
- Mailjet delivery failed (after accepting batch)
- Test emails blocked by recipient mail server

**Debug Steps**:
1. Check Mailjet dashboard for delivery status
2. Search spam folders
3. Verify recipient email addresses valid
4. Check Mailjet event logs for bounces

**Solution**: 
- Use valid, reachable email addresses for testing
- Whitelist Mailjet sender domain
- Monitor Mailjet delivery reports

---

### Issue 5: Duplicate Emails Received

**Symptoms**: Recipients receive multiple copies of same email

**Possible Causes**:
- Multiple API requests sent (user retried request)
- Bug in deduplication logic
- Email address appears in multiple organizer groups

**Debug Steps**:
```bash
# Check database for duplicate contact emails across partnerships
psql -d partners_connect -c "
  SELECT email, COUNT(*) as count
  FROM company_emails ce
  JOIN partnerships p ON ce.partnership_id = p.id
  WHERE p.event_id = (SELECT id FROM events WHERE slug = 'devlille-2025')
  GROUP BY email
  HAVING COUNT(*) > 1;
"
```

**Solution**: Deduplication only occurs **within organizer groups**, not globally. If same email appears in partnerships with different organizers, it receives multiple emails (one from each organizer).

---

## Test Cleanup

### Remove Test Data

```sql
-- Delete company emails
DELETE FROM company_emails WHERE partnership_id IN (
  SELECT id FROM partnerships WHERE event_id = '00000000-0000-0000-0000-000000000002'
);

-- Delete partnerships
DELETE FROM partnerships WHERE event_id = '00000000-0000-0000-0000-000000000002';

-- Delete sponsoring pack
DELETE FROM sponsoring_packs WHERE id = '00000000-0000-0000-0000-000000000020';

-- Delete companies
DELETE FROM companies WHERE id IN (
  '00000000-0000-0000-0000-000000000010',
  '00000000-0000-0000-0000-000000000011',
  '00000000-0000-0000-0000-000000000012'
);

-- Delete users
DELETE FROM users WHERE id IN (
  '00000000-0000-0000-0000-000000000003',
  '00000000-0000-0000-0000-000000000004'
);

-- Delete event
DELETE FROM events WHERE id = '00000000-0000-0000-0000-000000000002';

-- Delete Mailjet integration
DELETE FROM mailjet_integrations WHERE integration_id = '00000000-0000-0000-0000-000000000050';
DELETE FROM integrations WHERE id = '00000000-0000-0000-0000-000000000050';

-- Delete organisation
DELETE FROM organisations WHERE id = '00000000-0000-0000-0000-000000000001';
```

---

## Summary Checklist

**Before Testing**:
- [ ] Server running on localhost:8080
- [ ] PostgreSQL database populated with test data
- [ ] Mailjet integration configured with valid credentials
- [ ] JWT token obtained with edit permissions
- [ ] Bruno/Postman collection ready (optional)

**During Testing**:
- [ ] Run all 15 test scenarios
- [ ] Verify expected responses match actual responses
- [ ] Check Mailjet dashboard for sent messages
- [ ] Confirm recipients receive emails (for valid scenarios)

**After Testing**:
- [ ] Verify no errors in server logs
- [ ] Check database for orphaned records
- [ ] Clean up test data
- [ ] Document any unexpected behavior

**Success Criteria**:
- All 15 tests pass with expected responses
- Mailjet dashboard shows correct number of batches sent
- Email deduplication working correctly
- Authorization enforced properly
- Validation errors returned for invalid requests
