# Quickstart: Assign Organiser to Partnership

**Feature**: Organiser assignment to partnerships  
**Date**: November 22, 2025  
**Estimated Time**: 15-20 minutes

## Overview

This quickstart guide helps you test the organiser assignment feature locally. You'll create a partnership, assign an organiser, and verify the assignment through the API.

## Prerequisites

- Docker and Docker Compose installed
- Git repository cloned
- JDK 21 installed
- Bruno API client (optional, for testing)

## Quick Start

### 1. Start Local Environment

```bash
# From repository root
cd server
docker-compose up -d postgres

# Run migrations and start server
./gradlew run --no-daemon
```

Server will start on `http://localhost:8080`

### 2. Authenticate

```bash
# Get JWT token (replace with actual Google OAuth flow in production)
# For local testing, use the test user setup

export TOKEN="your-jwt-token-here"
```

### 3. Create Test Data

**Create Organization**:
```bash
curl -X POST http://localhost:8080/orgs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Organization",
    "slug": "test-org"
  }'
```

**Create Event**:
```bash
curl -X POST http://localhost:8080/orgs/test-org/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Event 2025",
    "slug": "test-event-2025"
  }'
```

**Create Company and Partnership** (use existing endpoints)

### 4. Assign Organiser

```bash
# Assign yourself as organiser
curl -X POST http://localhost:8080/orgs/test-org/events/test-event-2025/partnerships/{partnership-id}/organiser \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com"
  }'
```

**Expected Response**:
```json
{
  "partnership_id": "550e8400-e29b-41d4-a716-446655440000",
  "organiser": {
    "display_name": "Your Name",
    "picture_url": "https://example.com/photos/your-photo.jpg",
    "email": "your-email@example.com"
  }
}
```

### 5. Verify Assignment

```bash
# Get partnership with organiser information (use existing partnership endpoint)
curl -X GET http://localhost:8080/orgs/test-org/events/test-event-2025/partnerships/{partnership-id} \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (partnership includes organiser field):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "...",
  "companyId": "...",
  "organiser": {
    "display_name": "Your Name",
    "picture_url": "https://example.com/photos/your-photo.jpg",
    "email": "your-email@example.com"
  },
  ...
}
```

### 6. Remove Organiser

```bash
# Remove organiser assignment
curl -X DELETE http://localhost:8080/orgs/test-org/events/test-event-2025/partnerships/{partnership-id}/organiser \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response**:
```json
{
  "partnership_id": "550e8400-e29b-41d4-a716-446655440000",
  "organiser": null
}
```

## Testing Scenarios

### Scenario 1: Successful Assignment

**Setup**:
1. Create organization with two users (admin and organiser)
2. Grant both users edit permission for the organization
3. Create event and partnership

**Test**:
```bash
# Admin assigns organiser to partnership
curl -X POST http://localhost:8080/orgs/{org}/events/{event}/partnerships/{id}/organiser \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "organiser@example.com"}'
```

**Verify**: Response includes organiser User object with `display_name`, `picture_url`, and `email`

---

### Scenario 2: Non-Member Assignment (Should Fail)

**Setup**:
1. Create organization with admin user
2. Create separate user NOT in the organization
3. Create event and partnership

**Test**:
```bash
# Try to assign non-member as organiser
curl -X POST http://localhost:8080/orgs/{org}/events/{event}/partnerships/{id}/organiser \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "non-member@example.com"}'
```

**Verify**: Returns 403 Forbidden with error message about non-membership

---

### Scenario 3: Unauthorized Access (Should Fail)

**Setup**:
1. Create organization with admin user
2. Create separate user without edit permission
3. Create event and partnership

**Test**:
```bash
# User without edit permission tries to assign organiser
curl -X POST http://localhost:8080/orgs/{org}/events/{event}/partnerships/{id}/organiser \
  -H "Authorization: Bearer $NON_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "someone@example.com"}'
```

**Verify**: Returns 401 Unauthorized

---

### Scenario 4: Update Organiser Assignment

**Setup**:
1. Partnership with organiser already assigned

**Test**:
```bash
# Assign different organiser (replaces existing)
curl -X POST http://localhost:8080/orgs/{org}/events/{event}/partnerships/{id}/organiser \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "new-organiser@example.com"}'
```

**Verify**: Response shows new organiser User object, previous organiser replaced

---

### Scenario 5: Organiser Requires Edit Permission

**Setup**:
1. Create user in organization with `canEdit=false`
2. Create event and partnership

**Test**:
```bash
# Try to assign user without edit permission as organiser (should fail)
curl -X POST http://localhost:8080/orgs/{org}/events/{event}/partnerships/{id}/organiser \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "view-only-user@example.com"}'
```

**Verify**: Returns 403 Forbidden - organisers must have edit permission on the organization, not just membership

## Using Bruno API Client

Import the Bruno collection from `bruno/` directory:

1. Open Bruno
2. Import collection from `bruno/Events/partnership/`
3. Set environment variables:
   - `baseUrl`: `http://localhost:8080`
   - `token`: Your JWT token
   - `orgSlug`: `test-org`
   - `eventSlug`: `test-event-2025`
   - `partnershipId`: Your partnership UUID

4. Run requests in order:
   - `Assign Organiser.bru`
   - `Remove Organiser.bru`
   - Use existing `Get Partnership.bru` to view organiser information

## Database Verification

Connect to PostgreSQL and verify data:

```sql
-- Connect to database
psql -h localhost -U partners_connect -d partners_connect_dev

-- Check partnership organiser assignment
SELECT 
  p.id AS partnership_id,
  p.contact_name AS partner_contact,
  u.email AS organiser_email,
  u.name AS organiser_name,
  u.picture_url AS organiser_picture
FROM partnerships p
LEFT JOIN users u ON p.organiser_id = u.id
WHERE p.id = 'your-partnership-uuid';

-- Check user organization membership
SELECT 
  u.email,
  u.name,
  op.can_edit
FROM users u
JOIN organisation_permissions op ON u.id = op.user_id
JOIN organisations o ON op.organisation_id = o.id
WHERE o.slug = 'test-org';
```

## Troubleshooting

### Issue: 409 Conflict - User not a member

**Cause**: Target user doesn't have organization membership

**Solution**:
```bash
# Grant organization permission to user
curl -X POST http://localhost:8080/orgs/{org}/users/grant \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userEmails": ["organiser@example.com"]}'
```

### Issue: 401 Unauthorized

**Cause**: 
- Missing JWT token
- Invalid JWT token
- User lacks edit permission for organization

**Solution**:
1. Verify token is included in Authorization header
2. Check token expiration
3. Verify user has `canEdit=true` for the organization

### Issue: 404 Not Found

**Cause**:
- Partnership doesn't exist
- Partnership not in specified organization/event
- User email doesn't exist

**Solution**:
1. Verify partnership ID is correct
2. Check organization slug and event slug match partnership's event
3. Verify user with email exists in system

### Issue: Migration Not Running

**Cause**: Migration already applied or database schema out of sync

**Solution**:
```bash
# Check applied migrations
psql -h localhost -U partners_connect -d partners_connect_dev \
  -c "SELECT * FROM schema_migrations ORDER BY applied_at;"

# If organiser_id column missing, apply migration manually
psql -h localhost -U partners_connect -d partners_connect_dev \
  -c "ALTER TABLE partnerships ADD COLUMN organiser_id UUID REFERENCES users(id);"
```

## Validation Checklist

After running quickstart, verify:

- [ ] Partnership can be assigned an organiser
- [ ] Assigned organiser information is returned correctly (User object with display_name, picture_url, email)
- [ ] Non-member users cannot be assigned as organisers (403 error)
- [ ] Users without edit permission cannot assign organisers (401 error)
- [ ] Organiser assignment can be updated (replaces previous)
- [ ] Organiser assignment can be removed
- [ ] Partnership without organiser returns null organiser
- [ ] Database column `partnerships.organiser_id` exists and is nullable
- [ ] Foreign key constraint exists to `users.id`

## Next Steps

1. **Frontend Integration**: Add UI for organiser assignment in partnership management
2. **Partner View**: Display organiser contact information in partner dashboard
3. **Bulk Assignment**: Implement UI for assigning multiple partnerships to one organiser
4. **Analytics**: Track organiser workload and partnership distribution
5. **Notifications**: Consider adding notifications when organiser is assigned (currently silent per FR-013)

## Additional Resources

- [Feature Specification](./spec.md)
- [Data Model Documentation](./data-model.md)
- [API Contracts](./contracts/README.md)
- [Implementation Plan](./plan.md)
- Bruno Collection: `bruno/Events/partnership/`

## Support

- Check constitution compliance: See `.specify/memory/constitution.md`
- Review existing patterns: See `specs/*/quickstart.md` for similar features
- Run validation: `./gradlew check --no-daemon` from server directory
