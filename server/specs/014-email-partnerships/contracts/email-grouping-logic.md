# Email Grouping Logic: Organizer-Based Batching

**Feature**: 014-email-partnerships  
**Version**: 1.0  
**Status**: Complete

---

## Overview

Partnership emails are **grouped by assigned organizer** before sending to Mailjet. This ensures each batch has consistent sender information (From/CC fields), allowing organizers to send personalized emails from their own email address while maintaining event context.

**Key Behavior**:
- Partnerships **with** assigned organizer → From: organizer email, CC: event email
- Partnerships **without** organizer → From: event email, no CC
- Each organizer group creates **one Mailjet batch** (not one per partnership)
- Recipients are **deduplicated within each group** to avoid sending duplicate emails

---

## Grouping Algorithm

### Step 1: Fetch Partnerships with Emails and Organizers

**Input**: Event slug, optional filters (validated, paid, etc.)  
**Output**: `List<PartnershipWithEmails>`

```kotlin
data class PartnershipWithEmails(
    val partnershipId: UUID,
    val organiser: User?,  // null if no organizer assigned
    val emails: List<String>  // Contact emails for this partnership
)
```

**Database Query**:
```sql
SELECT 
    p.id AS partnership_id,
    u.id AS organiser_id,
    u.email AS organiser_email,
    u.firstname AS organiser_firstname,
    u.lastname AS organiser_lastname,
    ce.email AS contact_email
FROM partnerships p
LEFT JOIN users u ON p.organiser_user_id = u.id
JOIN company_emails ce ON ce.partnership_id = p.id
JOIN events e ON p.event_id = e.id
WHERE e.slug = :eventSlug
  AND [apply filters]
```

**Example Result**:
```kotlin
[
  PartnershipWithEmails(
    partnershipId = UUID("p1"),
    organiser = User(id=UUID("u1"), email="alice@example.com", firstname="Alice", lastname="Smith"),
    emails = ["contact1@companyA.com", "contact2@companyA.com"]
  ),
  PartnershipWithEmails(
    partnershipId = UUID("p2"),
    organiser = User(id=UUID("u1"), email="alice@example.com", firstname="Alice", lastname="Smith"),
    emails = ["contact3@companyB.com"]
  ),
  PartnershipWithEmails(
    partnershipId = UUID("p3"),
    organiser = null,
    emails = ["contact4@companyC.com"]
  ),
  PartnershipWithEmails(
    partnershipId = UUID("p4"),
    organiser = User(id=UUID("u2"), email="bob@example.com", firstname="Bob", lastname="Jones"),
    emails = ["contact5@companyD.com", "contact6@companyD.com"]
  )
]
```

---

### Step 2: Group by Organizer

**Algorithm**: Use Kotlin `groupBy` to partition partnerships by assigned organizer

```kotlin
val groupedByOrganiser: Map<User?, List<PartnershipWithEmails>> = 
    partnershipsWithEmails.groupBy { it.organiser }
```

**Result Structure**:
```kotlin
Map(
  User(alice@example.com) → [
    PartnershipWithEmails(p1, ...),
    PartnershipWithEmails(p2, ...)
  ],
  User(bob@example.com) → [
    PartnershipWithEmails(p4, ...)
  ],
  null → [
    PartnershipWithEmails(p3, ...)
  ]
)
```

**Key Property**: Map key is `User?` (nullable) to handle partnerships with no assigned organizer.

---

### Step 3: Deduplicate Recipients per Group

**Algorithm**: For each organizer group, collect all emails and remove duplicates

```kotlin
for ((organiser, partnerships) in groupedByOrganiser) {
    val allEmails: List<String> = partnerships.flatMap { it.emails }
    val uniqueRecipients: List<String> = allEmails.distinct()
    
    // uniqueRecipients is now the To: list for this batch
}
```

**Example**:
```kotlin
// Group 1: Alice (User u1)
Partnerships: [p1, p2]
All emails: ["contact1@companyA.com", "contact2@companyA.com", "contact3@companyB.com"]
Unique recipients: ["contact1@companyA.com", "contact2@companyA.com", "contact3@companyB.com"]  // 3 recipients

// Group 2: Bob (User u2)
Partnerships: [p4]
All emails: ["contact5@companyD.com", "contact6@companyD.com"]
Unique recipients: ["contact5@companyD.com", "contact6@companyD.com"]  // 2 recipients

// Group 3: No organizer (null key)
Partnerships: [p3]
All emails: ["contact4@companyC.com"]
Unique recipients: ["contact4@companyC.com"]  // 1 recipient
```

**Deduplication Scenario**:
```kotlin
// If two partnerships share the same contact email:
Partnerships: [
  PartnershipWithEmails(organiser=Alice, emails=["shared@example.com", "contact1@example.com"]),
  PartnershipWithEmails(organiser=Alice, emails=["shared@example.com", "contact2@example.com"])
]

All emails: ["shared@example.com", "contact1@example.com", "shared@example.com", "contact2@example.com"]
                                                            ↑ DUPLICATE
Unique recipients: ["shared@example.com", "contact1@example.com", "contact2@example.com"]
                    ↑ Only one occurrence
```

**Result**: `shared@example.com` receives only **one email**, not two.

---

### Step 4: Determine From/CC Fields per Group

**Rule**:
- **If organiser != null** (partnership has assigned organizer):
  - From: `organiser.email` with name `"${organiser.firstname} ${organiser.lastname}"`
  - CC: `event.contact.email`
- **If organiser == null** (partnership has no assigned organizer):
  - From: `event.contact.email` with name `event.name`
  - CC: (none)

**Example Calculation**:
```kotlin
// Event details (fetched once)
val event = Event(
    name = "DevLille 2025",
    contactEmail = "event@devlille.com"
)

// For Group 1: Alice
if (organiser != null) {
    fromEmail = organiser.email  // "alice@example.com"
    fromName = "${organiser.firstname} ${organiser.lastname}"  // "Alice Smith"
    ccEmail = event.contactEmail  // "event@devlille.com"
} else {
    fromEmail = event.contactEmail
    fromName = event.name
    ccEmail = null
}
```

**Resulting Sender Info**:
```kotlin
// Group 1 (Alice)
From: "Alice Smith" <alice@example.com>
CC: event@devlille.com

// Group 2 (Bob)
From: "Bob Jones" <bob@example.com>
CC: event@devlille.com

// Group 3 (No organizer)
From: "DevLille 2025" <event@devlille.com>
CC: (none)
```

---

### Step 5: Construct Mailjet Batch per Group

**Mailjet API Format** (v3.1 `/send` endpoint):
```json
{
  "Messages": [
    {
      "From": {"Email": "sender@example.com", "Name": "Sender Name"},
      "To": [
        {"Email": "recipient1@example.com"},
        {"Email": "recipient2@example.com"}
      ],
      "Cc": [{"Email": "cc@example.com"}],
      "Subject": "[Event Name] Subject",
      "HTMLPart": "<p>Email body HTML</p>"
    }
  ]
}
```

**Code**:
```kotlin
for ((organiser, partnerships) in groupedByOrganiser) {
    val uniqueRecipients = partnerships.flatMap { it.emails }.distinct()
    
    val (fromEmail, fromName, ccEmail) = if (organiser != null) {
        Triple(
            organiser.email,
            "${organiser.firstname} ${organiser.lastname}",
            event.contactEmail
        )
    } else {
        Triple(event.contactEmail, event.name, null)
    }
    
    val mailjetMessage = MailjetMessage(
        from = Contact(email = fromEmail, name = fromName),
        to = uniqueRecipients.map { Contact(email = it) },
        cc = ccEmail?.let { listOf(Contact(email = it)) },
        subject = "[${event.name}] ${request.subject}",
        htmlPart = request.body
    )
    
    mailjetProvider.send(MailjetBody(messages = listOf(mailjetMessage)), mailjetConfig)
}
```

**Example Mailjet Requests**:

**Batch 1** (Alice's partnerships):
```json
{
  "Messages": [
    {
      "From": {"Email": "alice@example.com", "Name": "Alice Smith"},
      "To": [
        {"Email": "contact1@companyA.com"},
        {"Email": "contact2@companyA.com"},
        {"Email": "contact3@companyB.com"}
      ],
      "Cc": [{"Email": "event@devlille.com"}],
      "Subject": "[DevLille 2025] Partnership Update",
      "HTMLPart": "<p>Dear Partner,</p><p>Thank you...</p>"
    }
  ]
}
```

**Batch 2** (Bob's partnerships):
```json
{
  "Messages": [
    {
      "From": {"Email": "bob@example.com", "Name": "Bob Jones"},
      "To": [
        {"Email": "contact5@companyD.com"},
        {"Email": "contact6@companyD.com"}
      ],
      "Cc": [{"Email": "event@devlille.com"}],
      "Subject": "[DevLille 2025] Partnership Update",
      "HTMLPart": "<p>Dear Partner,</p><p>Thank you...</p>"
    }
  ]
}
```

**Batch 3** (No organizer partnerships):
```json
{
  "Messages": [
    {
      "From": {"Email": "event@devlille.com", "Name": "DevLille 2025"},
      "To": [{"Email": "contact4@companyC.com"}],
      "Subject": "[DevLille 2025] Partnership Update",
      "HTMLPart": "<p>Dear Partner,</p><p>Thank you...</p>"
    }
  ]
}
```

---

## Complete Implementation Example

```kotlin
// Route Handler
fun sendPartnershipEmail(
    eventSlug: String,
    orgSlug: String,
    filters: PartnershipFilters,
    request: SendPartnershipEmailRequest,
    partnershipEmailRepository: PartnershipEmailRepository,
    eventRepository: EventRepository,
    notificationRepository: NotificationRepository
): SendPartnershipEmailResponse {
    // Step 1: Fetch partnerships with emails and organizers
    val partnershipsWithEmails = partnershipEmailRepository.getPartnershipsWithEmails(eventSlug, filters)
    
    if (partnershipsWithEmails.isEmpty()) {
        throw NotFoundException("No partnerships found matching the filters")
    }
    
    // Step 2: Fetch event details
    val event = eventRepository.findBySlug(eventSlug)
        ?: throw NotFoundException("Event not found: $eventSlug")
    
    // Step 3: Group by organizer (route orchestration)
    val groupedByOrganiser = partnershipsWithEmails.groupBy { it.organiser }
    
    var totalRecipients = 0
    
    // Step 4: Send email batches via NotificationRepository
    for ((organiser, partnerships) in groupedByOrganiser) {
        val recipients = partnerships.flatMap { it.emails }.distinct()
        if (recipients.isEmpty()) continue
        
        totalRecipients += recipients.size
        
        // Determine sender info
        val fromContact = if (organiser != null) {
            EmailContact(
                email = organiser.email,
                name = "${organiser.firstname} ${organiser.lastname}"
            )
        } else {
            EmailContact(
                email = event.contactEmail,
                name = event.name
            )
        }
        
        val toContacts = recipients.map { EmailContact(email = it) }
        val ccContacts = if (organiser != null) {
            listOf(EmailContact(email = event.contactEmail))
        } else null
        
        // Send via notification repository
        val success = notificationRepository.sendMail(
            orgSlug = orgSlug,
            from = fromContact,
            to = toContacts,
            cc = ccContacts,
            subject = "[${event.name}] ${request.subject}",
            htmlBody = request.body
        )
        
        if (!success) {
            throw ServiceUnavailableException("Email service is currently unavailable. Please try again later.")
        }
    }
    
    return SendPartnershipEmailResponse(recipients = totalRecipients)
}

// NotificationRepository Implementation (enhanced)
class NotificationRepositoryImpl(
    private val integrationRepository: IntegrationRepository,  // Only for config lookup
    private val mailjetProvider: MailjetProvider
) : NotificationRepository {
    
    override suspend fun sendMail(
        orgSlug: String,
        from: EmailContact,
        to: List<EmailContact>,
        cc: List<EmailContact>?,
        subject: String,
        htmlBody: String
    ): Boolean {
        // Check if Mailjet gateway configured
        val mailjetConfig = integrationRepository.getMailjetConfig(orgSlug)
            ?: throw NotFoundException("Email gateway not configured for organisation")
        
        // Send via Mailjet provider
        
        var totalRecipients = 0
        
        // Send batch per organizer group
        for ((organiser, partnerships) in groupedPartnerships) {
            // Deduplicate recipients
            val uniqueRecipients = partnerships.flatMap { it.emails }.distinct()
            
            if (uniqueRecipients.isEmpty()) continue
            
            totalRecipients += uniqueRecipients.size
            
            // Determine sender info
            val (fromEmail, fromName, ccEmail) = if (organiser != null) {
                Triple(
                    organiser.email,
                    "${organiser.firstname} ${organiser.lastname}",
                    eventContactEmail
                )
            } else {
                Triple(eventContactEmail, eventName, null)
            }
            
            // Construct Mailjet message
            val message = MailjetMessage(
                from = Contact(email = fromEmail, name = fromName),
                to = uniqueRecipients.map { Contact(email = it) },
                cc = ccEmail?.let { listOf(Contact(email = it)) },
                subject = "[$eventName] $subject",
                htmlPart = body
            )
            
            // Send batch
            val success = mailjetProvider.send(
                MailjetBody(messages = listOf(message)),
                mailjetConfig
            )
            
            if (!success) {
                throw ServiceUnavailableException("Email service is currently unavailable. Please try again later.")
            }
        }
        
        return totalRecipients
    }
}
```

---

## Edge Cases & Special Scenarios

### Case 1: All Partnerships Have Same Organizer

**Scenario**: 10 partnerships all assigned to Alice

**Behavior**:
- Single group created (Map key = Alice)
- All partnership emails collected and deduplicated
- **Only 1 Mailjet batch** sent (not 10)

**Benefit**: Reduces API calls and avoids rate limiting

---

### Case 2: All Partnerships Have No Organizer

**Scenario**: 5 partnerships with `organiser_user_id = null`

**Behavior**:
- Single group created (Map key = null)
- All emails sent from `event.contact.email`
- **Only 1 Mailjet batch** sent

---

### Case 3: Mixed Organizer Assignment

**Scenario**:
- 3 partnerships assigned to Alice
- 2 partnerships assigned to Bob
- 1 partnership with no organizer

**Behavior**:
- 3 groups created (Alice, Bob, null)
- **3 Mailjet batches** sent

**Example**:
```
Batch 1: From Alice → 8 recipients (partnerships p1, p2, p3)
Batch 2: From Bob → 3 recipients (partnerships p4, p5)
Batch 3: From Event → 2 recipients (partnership p6)
```

---

### Case 4: Duplicate Emails Across Partnerships

**Scenario**:
- Partnership P1 (Alice): emails = ["shared@example.com", "contact1@example.com"]
- Partnership P2 (Alice): emails = ["shared@example.com", "contact2@example.com"]

**Behavior**:
- Both partnerships in same group (Alice)
- Emails collected: `["shared@example.com", "contact1@example.com", "shared@example.com", "contact2@example.com"]`
- After `.distinct()`: `["shared@example.com", "contact1@example.com", "contact2@example.com"]`
- **Total recipients: 3** (not 4)

**Result**: `shared@example.com` receives only one email

---

### Case 5: Partnership with No Contact Emails

**Scenario**: Partnership exists but `company_emails` table has zero rows for that partnership

**Behavior**:
- Partnership NOT included in query results (JOIN condition fails)
- No impact on grouping or sending
- If ALL partnerships have zero emails → `partnershipsWithEmails.isEmpty() == true` → 404 Not Found

---

### Case 6: Single Partnership with Multiple Organizers (NOT POSSIBLE)

**Database Constraint**: `partnerships.organiser_user_id` is a single foreign key (NOT many-to-many)

**Result**: Each partnership can have **at most one assigned organizer**.

---

## Performance Characteristics

### Time Complexity

- **Grouping**: O(n) where n = number of partnerships
- **Deduplication per group**: O(m) where m = number of emails in group
- **Overall**: O(n + m) linear time

### Space Complexity

- **Grouping map**: O(n) partnerships stored
- **Deduplication sets**: O(m) unique emails stored
- **Overall**: O(n + m) linear space

### Database Impact

- **Single query**: Fetches all partnerships + emails + organizers in one JOIN
- **No N+1 problem**: All data retrieved upfront, no subsequent queries per partnership

### Mailjet API Calls

- **Minimum calls**: 1 (all partnerships assigned to same organizer or all have no organizer)
- **Maximum calls**: n (each partnership assigned to different organizer)
- **Expected average**: 3-5 batches (typical organizer distribution)

---

## Testing Strategy

### Unit Tests for Grouping Logic

**Test 1**: Group partnerships by organizer
```kotlin
@Test
fun `should group partnerships by assigned organizer`() {
    val alice = User(id = UUID.randomUUID(), email = "alice@example.com", firstname = "Alice", lastname = "Smith")
    val bob = User(id = UUID.randomUUID(), email = "bob@example.com", firstname = "Bob", lastname = "Jones")
    
    val partnerships = listOf(
        PartnershipWithEmails(UUID.randomUUID(), alice, listOf("a@example.com")),
        PartnershipWithEmails(UUID.randomUUID(), alice, listOf("b@example.com")),
        PartnershipWithEmails(UUID.randomUUID(), bob, listOf("c@example.com")),
        PartnershipWithEmails(UUID.randomUUID(), null, listOf("d@example.com"))
    )
    
    val grouped = partnerships.groupBy { it.organiser }
    
    assertEquals(3, grouped.size)  // 3 groups: Alice, Bob, null
    assertEquals(2, grouped[alice]?.size)  // Alice has 2 partnerships
    assertEquals(1, grouped[bob]?.size)   // Bob has 1 partnership
    assertEquals(1, grouped[null]?.size)  // No organizer has 1 partnership
}
```

**Test 2**: Deduplicate emails within group
```kotlin
@Test
fun `should deduplicate emails within organizer group`() {
    val alice = User(id = UUID.randomUUID(), email = "alice@example.com", firstname = "Alice", lastname = "Smith")
    
    val partnerships = listOf(
        PartnershipWithEmails(UUID.randomUUID(), alice, listOf("shared@example.com", "a@example.com")),
        PartnershipWithEmails(UUID.randomUUID(), alice, listOf("shared@example.com", "b@example.com"))
    )
    
    val allEmails = partnerships.flatMap { it.emails }
    val uniqueEmails = allEmails.distinct()
    
    assertEquals(4, allEmails.size)  // 4 total (with duplicate)
    assertEquals(3, uniqueEmails.size)  // 3 unique: shared, a, b
    assertTrue(uniqueEmails.contains("shared@example.com"))
    assertTrue(uniqueEmails.contains("a@example.com"))
    assertTrue(uniqueEmails.contains("b@example.com"))
}
```

**Test 3**: Determine sender info based on organizer
```kotlin
@Test
fun `should use organizer email as From when organizer assigned`() {
    val alice = User(id = UUID.randomUUID(), email = "alice@example.com", firstname = "Alice", lastname = "Smith")
    val event = Event(name = "DevLille 2025", contactEmail = "event@devlille.com")
    
    val (fromEmail, fromName, ccEmail) = if (alice != null) {
        Triple(alice.email, "${alice.firstname} ${alice.lastname}", event.contactEmail)
    } else {
        Triple(event.contactEmail, event.name, null)
    }
    
    assertEquals("alice@example.com", fromEmail)
    assertEquals("Alice Smith", fromName)
    assertEquals("event@devlille.com", ccEmail)
}

@Test
fun `should use event email as From when no organizer assigned`() {
    val event = Event(name = "DevLille 2025", contactEmail = "event@devlille.com")
    
    val (fromEmail, fromName, ccEmail) = if (null != null) {
        Triple(null.email, "${null.firstname} ${null.lastname}", event.contactEmail)
    } else {
        Triple(event.contactEmail, event.name, null)
    }
    
    assertEquals("event@devlille.com", fromEmail)
    assertEquals("DevLille 2025", fromName)
    assertNull(ccEmail)
}
```

---

## Integration Test Scenarios

**Test 1**: Send email to partnerships with mixed organizer assignment
```kotlin
@Test
fun `should send separate batches per organizer group`() {
    // Setup: 3 partnerships (2 for Alice, 1 for Bob)
    // Assert: 2 Mailjet API calls made (not 3)
    // Verify: Each batch has correct From/CC fields
}
```

**Test 2**: Send email to partnerships all assigned to same organizer
```kotlin
@Test
fun `should send single batch when all partnerships have same organizer`() {
    // Setup: 5 partnerships all assigned to Alice
    // Assert: 1 Mailjet API call made (not 5)
    // Verify: All 5 partnership emails in single To: list
}
```

**Test 3**: Verify deduplication across partnerships
```kotlin
@Test
fun `should not send duplicate emails to shared contact`() {
    // Setup: 2 partnerships sharing same contact email
    // Assert: Contact receives only 1 email (not 2)
    // Verify: Response recipients count reflects deduplicated total
}
```

---

## Summary

**Grouping Key**: Partnership organizer (User entity, nullable)  
**Group Count**: Variable (1 to n, where n = number of unique organizers + 1 if any have no organizer)  
**Deduplication Scope**: Within each organizer group (not global across all groups)  
**Mailjet Batches**: One per organizer group  
**Sender Logic**: Organizer email (if assigned) or event email (if not assigned)  
**CC Field**: Event email (only when organizer assigned)  
**Performance**: O(n) time and space, single database query
