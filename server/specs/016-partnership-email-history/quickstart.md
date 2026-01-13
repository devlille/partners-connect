# Quickstart Guide: Partnership Email History

**Feature**: 016-partnership-email-history  
**Last Updated**: January 10, 2026

## Overview

This feature automatically logs all emails sent to partnerships and provides an API endpoint for organisers to retrieve the complete email history. Every email includes per-recipient delivery status for full transparency.

## Prerequisites

- Running PostgreSQL database
- Mailjet integration configured for event
- Valid OAuth token with organiser permissions

## Quick Start

### 1. Send an Email (Existing Workflow)

Emails are automatically logged when sent through existing endpoints. No code changes needed in client applications.

```bash
# Example: Approve partnership (sends notification email)
curl -X POST \
  https://api.partners-connect.com/orgs/devlille/events/devfest-2026/partnerships/a3bb189e-8bf9-3888-9912-ace4e6543002/approve \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Result**: Email sent to partnership contacts + automatically logged in history.

### 2. Retrieve Email History

```bash
curl -X GET \
  "https://api.partners-connect.com/orgs/devlille/events/devfest-2026/partnerships/a3bb189e-8bf9-3888-9912-ace4e6543002/email-history?page=0&page_size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "partnershipId": "a3bb189e-8bf9-3888-9912-ace4e6543002",
      "sentAt": "2026-01-10T14:30:00",
      "senderEmail": "organiser@devlille.fr",
      "subject": "[DevFest Lille 2026][Acme Corp] Partnership Approved",
      "bodyPlainText": "Dear Acme Corp, your partnership has been approved...",
      "overallStatus": "sent",
      "triggeredBy": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "recipients": [
        {
          "email": "partnerships@acmecorp.com",
          "status": "sent"
        },
        {
          "email": "organiser@devlille.fr",
          "status": "sent"
        }
      ]
    }
  ],
  "page": 0,
  "page_size": 20,
  "total": 1
}
```

### 3. Paginate Through History

For partnerships with many emails:

```bash
# Get page 2 (records 21-40)
curl -X GET \
  "https://api.partners-connect.com/orgs/devlille/events/devfest-2026/partnerships/{id}/email-history?page=1&page_size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Common Use Cases

### View Recent Communications

Get the most recent 10 emails:

```bash
curl -X GET \
  ".../email-history?page=0&page_size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Emails are returned newest first.

### Check Delivery Status

See which recipients received an email:

```json
{
  "overallStatus": "partial",
  "recipients": [
    {"email": "success@company.com", "status": "sent"},
    {"email": "failed@invalid", "status": "failed"}
  ]
}
```

**Status meanings**:
- `sent` - Successfully delivered to all recipients
- `failed` - All deliveries failed
- `partial` - Mixed success/failure

### Audit Email Content

Retrieve full email body (plain text):

```json
{
  "subject": "[DevFest][Acme] Partnership Approved",
  "bodyPlainText": "Dear Acme Corp,\n\nYour partnership application...",
  "senderEmail": "organiser@devlille.fr"
}
```

HTML content is automatically converted to plain text for storage.

## Testing Locally

### Setup Test Environment

```bash
cd server
docker-compose up -d  # Start PostgreSQL
./gradlew run --no-daemon  # Start server on port 8080
```

### 1. Create Test Data

```bash
# Create organization, event, and partnership
# (Use existing test scripts or API calls)
```

### 2. Send Test Email

```bash
# Trigger any partnership action that sends email
# For example: approve partnership, send custom message, etc.
```

### 3. Verify History

```bash
curl -X GET \
  "http://localhost:8080/orgs/test-org/events/test-event/partnerships/{id}/email-history" \
  -H "Authorization: Bearer YOUR_TEST_TOKEN"
```

### 4. Check Database

```sql
-- Connect to PostgreSQL
SELECT * FROM partnership_email_history WHERE partnership_id = 'YOUR_PARTNERSHIP_ID';
SELECT * FROM recipient_delivery_status WHERE email_history_id = 'YOUR_HISTORY_ID';
```

## Validation Checklist

✅ **Automatic Logging**:
- [ ] Emails appear in history immediately after sending
- [ ] All fields populated correctly (sender, subject, body, recipients)
- [ ] Per-recipient status accurately reflects Mailjet response

✅ **Retrieval**:
- [ ] GET endpoint returns emails newest first
- [ ] Pagination works correctly with page/limit parameters
- [ ] Empty history returns empty array (not error)

✅ **Authorization**:
- [ ] Unauthenticated requests return 401
- [ ] Non-organisers return 403
- [ ] Organisers can access history for their events only

✅ **Data Integrity**:
- [ ] History records are immutable (cannot be updated/deleted)
- [ ] HTML content converted to plain text correctly
- [ ] Per-recipient status tracked accurately

✅ **Performance**:
- [ ] Response time <2 seconds for partnerships with <1000 emails
- [ ] Pagination prevents timeouts with large histories

## Error Scenarios

### Invalid Pagination Parameters

```bash
curl -X GET ".../email-history?page=-1&page_size=200"
# 400 Bad Request: "Invalid query parameter: page must be >= 0"
```

### Partnership Not Found

```bash
curl -X GET ".../partnerships/nonexistent-uuid/email-history"
# 404 Not Found: "Partnership with ID nonexistent-uuid not found"
```

### Unauthorized Access

```bash
curl -X GET ".../email-history"
# 401 Unauthorized: "Unauthorized"
```

### No Permissions

```bash
# User tries to access another organization's partnership
curl -X GET ".../other-org/.../email-history" -H "Authorization: Bearer TOKEN"
# 403 Forbidden: "User does not have permission for this event"
```

## Integration Notes

### Frontend Integration

```typescript
// Fetch email history
async function getEmailHistory(
  orgSlug: string,
  eventSlug: string,
  partnershipId: string,
  page: number = 0,
  pageSize: number = 20
) {
  const response = await fetch(
    `/orgs/${orgSlug}/events/${eventSlug}/partnerships/${partnershipId}/email-history?page=${page}&page_size=${pageSize}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    }
  );
  return response.json();
}

// Display in UI
const history = await getEmailHistory("devlille", "devfest-2026", partnershipId);
console.log(`Total emails: ${history.total}`);
history.items.forEach(email => {
  console.log(`${email.sentAt}: ${email.subject} (${email.overallStatus})`);
});
```

### Mobile Integration

```kotlin
// Kotlin example
data class EmailHistoryResponse(
    val items: List<EmailHistory>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long
)

suspend fun getEmailHistory(
    partnershipId: String,
    page: Int = 0,
    pageSize: Int = 20
): EmailHistoryResponse {
    return httpClient.get(
        "/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/email-history"
    ) {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()
}
```

## Troubleshooting

### No emails in history

**Symptom**: Empty array returned even after sending emails

**Possible causes**:
- Emails sent before feature deployed (no historical data)
- Wrong partnership ID in request
- Email sending failed (check logs)

**Solution**: Verify partnership ID, check that emails are actually being sent

### Missing recipient status

**Symptom**: Recipients array is empty or incomplete

**Possible causes**:
- Mailjet response parsing error
- Database foreign key constraint issue

**Solution**: Check server logs for Mailjet response, verify database integrity

### Slow response time

**Symptom**: GET request takes >2 seconds

**Possible causes**:
- Large history without pagination (use smaller limit)
- Missing database index

**Solution**: Verify index exists on (partnership_id, sent_at), use pagination

## Next Steps

- ✅ Feature is ready for use
- 📊 Monitor response times in production
- 📧 Consider adding email search/filter capabilities in future
- 🔍 Add analytics on email delivery success rates

## Support

For issues or questions:
- Check server logs: `docker logs partners-connect-server`
- Review OpenAPI documentation: `/swagger`
- Consult data model: [data-model.md](data-model.md)
- Review API contract: [contracts/get_partnership_email_history.md](contracts/get_partnership_email_history.md)
