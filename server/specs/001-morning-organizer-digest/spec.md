# Feature Specification: Morning Organiser Daily Digest

**Feature Branch**: `001-morning-organizer-digest`
**Created**: March 5, 2026
**Status**: Draft
**Input**: User description: "Every morning at 8h, I would like to notify organiser of the event on their messaging integration about things to do today: If a company have all necessary information to generate a assignment, we should ask to generate it; If a company have all necessary information to generate the quote, we should ask to generate it; If a company is planned to be communicated on social media today, we should post a reminder to avoid to forget it"

## Clarifications

### Session 2026-03-05

- Q: Does a social media publication date field already exist on the partnership or company data model, and if so where? → A: The field `communication_publication_date` already exists on the partnership record.
- Q: What does "confirmed sponsoring pack" mean in the data model? → A: A pack is confirmed when the partnership's `validated_at` datetime field is non-null.
- Q: What defines an "active" event for the digest? → A: An event is active when its `start_time` has not yet been reached (i.e. `start_time` is in the future relative to the digest run time).
- Q: How is "already generated" tracked for agreements and quotes in the data model? → A: `agreement_url` on the partnership record — non-null means the agreement has been generated. `quote_pdf_url` on the billings record — non-null means the quote has been generated.
- Q: Which address fields are required for quote readiness? → A: Same company address fields used for the agreement: `address`, `zip_code`, `city`, `country`.

## Assumptions

- **Messaging integration**: The messaging integration referred to is the existing Slack integration configured per event. Each event has a Slack channel where notifications are already sent; the digest is sent to that same channel.
- **Digest timing**: The digest is sent at 08:00 UTC. Events do not have individual timezone settings; all events share the same UTC-based schedule.
- **Digest scope**: Each event has its own independent digest. If an organisation manages multiple events, each event whose `start_time` has not yet been reached produces a separate digest sent to that event's Slack channel.
- **Event enumeration**: The server endpoint processes one event per call (`POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`). The external scheduler is responsible for maintaining the list of active event IDs and calling the endpoint once per event. The server does not expose an event-discovery endpoint; event ID enumeration is the caller's responsibility.
- **No-op when empty**: If there are no actionable items across all three categories for a given event, no message is sent for that event on that day.
- **Active partnerships only**: Only partnerships that have not been declined are considered for the digest. Declined partnerships (those with a non-null `declinedAt`) are excluded from all digest sections.
- **Agreement readiness criteria**: A partnership is considered ready for agreement generation when the company has a name, SIRET number, full address (address, zip code, city, country), contact name, and contact role, and the partnership's `validated_at` is non-null (pack confirmed). The agreement has not yet been generated (i.e. `agreement_url` on the partnership record is null).
- **Quote readiness criteria**: A partnership is considered ready for quote generation when the company has a name, SIRET number, and full address (`address`, `zip_code`, `city`, `country`), the partnership's `validated_at` is non-null (pack confirmed), and the selected pack has a defined price. The quote has not yet been generated (i.e. `quote_pdf_url` on the billing record linked to the partnership is null or no billing record exists).
- **Social media communication date**: The `communication_publication_date` field on the partnership record records the planned date the company will be announced on the event's social media channels. The digest evaluates this field against today's UTC date.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receive agreement-readiness reminders (Priority: P1)

Each morning, the organiser's Slack channel for an event automatically receives a message listing all partner companies whose details are complete enough to generate a partnership agreement (contract document), but for which the agreement has not yet been produced. The organiser can act immediately by generating those agreements, keeping the partnership paperwork on track without manually checking each record.

**Why this priority**: Agreement generation is a critical step in the partnership workflow — companies are waiting for their contract before they can complete payment and fulfil their sponsorship obligations. Missing this step has a direct financial and operational impact.

**Independent Test**: Can be fully tested by configuring an event with a Slack channel, creating several partnerships — some with all required company and agreement data, others missing data — and waiting for (or manually triggering) the 08:00 digest run. Verify that only the complete-and-not-yet-generated partnerships appear in the message, and that the message is absent when no such partnerships exist. Delivers standalone value as an automatic paperwork reminder.

**Acceptance Scenarios**:

1. **Given** an event has one partnership with all required agreement data and no existing agreement document, **When** the digest runs at 08:00, **Then** the Slack message includes that company with a prompt to generate the agreement
2. **Given** an event has one partnership missing required agreement data (e.g. no SIRET number), **When** the digest runs at 08:00, **Then** that company is NOT included in the agreement section
3. **Given** an event has one partnership with all required data but an agreement has already been generated, **When** the digest runs at 08:00, **Then** that company is NOT included in the agreement section
4. **Given** an event has no partnerships ready for agreement generation, **When** the digest runs at 08:00 and there are also no quote or social-media reminders, **Then** no message is sent to the Slack channel
5. **Given** an event has a declined partnership with complete agreement data, **When** the digest runs at 08:00, **Then** that company is NOT included in the digest

---

### User Story 2 - Receive quote-readiness reminders (Priority: P2)

Each morning, the organiser's Slack channel receives a message listing all partner companies whose billing details are sufficiently complete to generate a financial quote, but for which no quote has yet been produced. The organiser can immediately generate the quote and advance the billing cycle without manually auditing each partnership.

**Why this priority**: Quote generation is a prerequisite for invoicing and payment collection. Companies that are ready for a quote but have not received one represent stalled cash flow. This is slightly lower priority than assignments because a quote may logically come after the assignment, but it is still financially critical.

**Independent Test**: Can be fully tested by configuring an event with a Slack channel, creating partnerships with varying degrees of billing data completeness, and triggering the digest. Verify that only partnerships with complete billing data and no existing quote appear in the message.

**Acceptance Scenarios**:

1. **Given** an event has one partnership with all required billing data and no existing quote, **When** the digest runs at 08:00, **Then** the Slack message includes that company with a prompt to generate the quote
2. **Given** an event has one partnership missing required billing data (e.g. no billing address), **When** the digest runs at 08:00, **Then** that company is NOT included in the quote section
3. **Given** an event has one partnership with all required data but a quote has already been generated, **When** the digest runs at 08:00, **Then** that company is NOT included in the quote section
4. **Given** an event has partnerships ready for both agreement and quote generation, **When** the digest runs at 08:00, **Then** the Slack message includes both sections in a single message

---

### User Story 3 - Receive social media announcement reminders (Priority: P3)

Each morning, the organiser's Slack channel receives a message listing all partner companies whose planned social media announcement date is today. This prevents organisers from accidentally missing a scheduled communication that was agreed with the partner.

**Why this priority**: Social media announcements are time-sensitive commitments made to sponsors. Missing an agreed announcement date damages partner relationships, but this is recoverable and has lower operational urgency than paperwork generation, hence P3.

**Independent Test**: Can be fully tested by setting the social media publication date on a partnership to today's date, triggering the digest, and verifying the company appears in the social media section. Also verify that a partnership with a date of yesterday or tomorrow does not appear.

**Acceptance Scenarios**:

1. **Given** a partnership has a social media publication date equal to today, **When** the digest runs at 08:00, **Then** the Slack message includes that company in the social media reminder section
2. **Given** a partnership has a social media publication date in the past or future, **When** the digest runs at 08:00, **Then** that company does NOT appear in the social media reminder section
3. **Given** a partnership has no social media publication date set, **When** the digest runs at 08:00, **Then** that company does NOT appear in the social media reminder section
4. **Given** a declined partnership has a social media publication date equal to today, **When** the digest runs at 08:00, **Then** that company does NOT appear in the digest

---

### Edge Cases

- What happens when the Slack channel is misconfigured or the webhook is invalid? The digest send attempt fails silently with an internal error log; no retry is performed.
- What happens when a company appears in multiple sections (e.g. ready for both assignment and quote)? The company appears in each relevant section independently; sections are not deduplicated.
- What happens if the digest job is delayed and runs after 08:00? The digest still runs for that day using today's date; social media reminders are still correct as long as they run before end of day.
- What happens when an event has no Slack channel configured? The digest is skipped for that event; no error is raised.
- What happens when there are multiple events in the same organisation? Each event is processed independently; each sends its own digest to its own channel.
- What happens if `communication_publication_date` is null on a partnership? The partnership is silently excluded from the social media section; no error is raised.
- What happens when the digest criteria cross midnight boundaries (e.g. timezone edge cases)? By assumption, all dates are evaluated against UTC midnight, so "today" is always the current UTC date at job execution time.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST run a scheduled job every day at 08:00 UTC that processes all events whose `start_time` has not yet been reached at the time of execution
- **FR-002**: For each event whose `start_time` has not yet been reached, the system MUST evaluate all non-declined partnerships against the three digest categories
- **FR-003**: System MUST send a single consolidated Slack message per event when at least one actionable item exists across all categories; if no actionable items exist, no message is sent
- **FR-004**: The Slack message MUST include a section for agreement-readiness listing all partnerships where the company has a name, SIRET number, full address (address, zip code, city, country), contact name, and contact role, the partnership `validated_at` is non-null, and `agreement_url` on the partnership is null
- **FR-005**: The Slack message MUST include a section for quote-readiness listing all partnerships where the company has a name, SIRET number, and full address (`address`, `zip_code`, `city`, `country`), the partnership `validated_at` is non-null, the selected pack has a defined price, and `quote_pdf_url` on the associated billing record is null (or no billing record exists yet)
- **FR-006**: The Slack message MUST include a section for social media reminders listing all partnerships whose social media publication date equals today's UTC date
- **FR-007**: System MUST exclude declined partnerships (those with a non-null `declinedAt`) from all three digest sections
- **FR-008**: Each company listed in the message MUST include the company name and a direct link to navigate to the partnership in the platform
- **FR-009**: Sections with zero matching items MUST be omitted from the message entirely (no empty sections)
- **FR-010**: System MUST send the digest to the Slack channel configured for the event; if no channel is configured, the event is silently skipped
- **FR-011**: System MUST log an error if the Slack message delivery fails, but MUST NOT retry or block processing of other events
- **FR-012**: System MUST evaluate the `communication_publication_date` field on the partnership record against today's UTC date to determine social media reminder eligibility

### Key Entities

- **Partnership**: Represents a sponsorship relationship between a company and an event. Relevant attributes: `agreement_url` (nullable — non-null means the agreement has been generated), `declinedAt` (nullable datetime), `validated_at` (nullable datetime — non-null means the sponsoring pack has been confirmed), `communication_publication_date` (nullable date — the planned social media announcement date for this partnership), `contact_name` (used for agreement readiness), `contact_role` (used for agreement readiness).
- **Billing**: A billing record linked to a partnership. Relevant attributes: `quote_pdf_url` (nullable — non-null means the quote has been generated).
- **Company**: A sponsor company attached to a partnership. Relevant attributes: `name`, `siret`, `address`, `zip_code`, `city`, `country` (used for both agreement and quote readiness).
- **Sponsoring Pack**: The sponsorship package selected for a partnership. Relevant attributes: defined price.
- **Event**: The conference or meetup being organised. Relevant attributes: `start_time` (datetime — events with a future `start_time` are included in the digest), configured Slack channel/webhook.
- **Digest Job**: A scheduled process that runs at 08:00 UTC, iterates over all active events, evaluates partnerships, and dispatches Slack messages.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every active event with at least one actionable item receives a Slack digest message by 08:05 UTC each morning, with no manual trigger required
- **SC-002**: Zero actionable items are missed — a partnership that meets the readiness criteria on a given day appears in the digest for that day with 100% recall
- **SC-003**: No false positives — a partnership that does not meet the readiness criteria, or for which the required document already exists, does NOT appear in the digest
- **SC-004**: Organisers can take action directly from the Slack message — each listed company includes a link that navigates to the relevant partnership record without additional navigation steps
- **SC-005**: Events with no actionable items produce no noise — the Slack channel receives zero messages on days when there is nothing to act on
- **SC-006**: A missed or delayed delivery on one event does not affect the digest delivery of any other event
