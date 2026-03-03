# Feature Specification: Filter Partnerships by Declined Status

**Feature Branch**: `018-filter-declined-partnerships`
**Created**: March 2, 2026
**Status**: Draft
**Input**: User description: "I would like to improve the existing service which provide partnership for an event with a new filter. I would like to be able to apply a filter to include or exclude partnership declined. Warning: Another service is using the same filtering, it is the one about emailing based on same filter than the partnership one and we need to add the filter there too."

## Clarifications

### Session 2026-03-02

- Q: Does the current GET partnerships endpoint already exclude declined partnerships, or does it currently return them? → A: Currently returned — the endpoint returns all partnerships including declined ones. The new default (exclude declined) is an intentional breaking change to the default behaviour.
- Q: What are the possible status values for a partnership? → A: There is no status enum. A partnership is considered declined when its `declinedAt` datetime column is non-null. Note: `suggestionDeclinedAt` is a separate field for declining a pack suggestion and is unrelated to this filter.
- Q: Should the `filter[declined]` entry be added to the `filters` metadata in the partnership list response? → A: Yes — add `filter[declined]` with `type: "boolean"` to the filters metadata block to maintain the discoverability contract from spec 015.

## Assumptions

- **Default behaviour**: By default (when no `filter[declined]` parameter is provided), declined partnerships are **excluded** from results. **This is a breaking change from the current behaviour**, where declined partnerships are returned alongside all others. Existing callers that do not pass the filter will receive fewer results after this change is deployed. Consumers who need the old behaviour must explicitly pass `filter[declined]=true`.
- **Filter semantics**: The filter accepts a boolean value (`true` to include declined partnerships, `false` to explicitly exclude them). When omitted or set to `false`, declined partnerships are not returned.
- **Declined status**: A partnership is considered declined when its `declinedAt` datetime column is non-null. There is no status enum field; decline is recorded as a timestamp. The filter operates on the nullability of `declinedAt` only — it has no relation to `suggestionDeclinedAt`, which tracks the separate act of declining a pack suggestion.
- **Filter consistency**: The same filter parameter name and semantics are applied identically to both the partnership list endpoint and the email endpoint, consistent with the design contract established in spec `014-email-partnerships`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Declined Partnerships in Partnership List (Priority: P1)

Event organisers sometimes need a full picture of all partnership interactions for an event, including those that were declined. The current partnership list silently hides declined partnerships, making it impossible to review past decisions, audit outcomes, or identify previously interested companies. By adding a `filter[declined]` parameter to the listing endpoint, organisers can opt in to seeing declined partnerships when needed.

**Why this priority**: This is the core value of the feature. It directly answers the stated need and delivers immediate value to organisers who need to review declined partnerships. All other stories depend on this semantic being well-defined.

**Independent Test**: Can be fully tested by authenticating as an organiser with view permissions, creating a mix of active and declined partnerships for an event, then calling the GET partnerships endpoint with `filter[declined]=true` and verifying that declined partnerships appear in the results. Can also verify that omitting the filter (or setting it to `false`) returns only non-declined partnerships. Delivers standalone value as a visibility improvement in the partnership management view.

**Acceptance Scenarios**:

1. **Given** an event has both active and declined partnerships, **When** an organiser requests the partnership list without the `filter[declined]` parameter, **Then** only non-declined partnerships are returned
2. **Given** an event has both active and declined partnerships, **When** an organiser requests the partnership list with `filter[declined]=false`, **Then** only non-declined partnerships are returned
3. **Given** an event has both active and declined partnerships, **When** an organiser requests the partnership list with `filter[declined]=true`, **Then** both active and declined partnerships are returned
4. **Given** an event has only declined partnerships, **When** an organiser requests the partnership list with `filter[declined]=true`, **Then** all declined partnerships are returned
5. **Given** an event has only declined partnerships, **When** an organiser requests the partnership list without the filter, **Then** an empty list is returned with HTTP 200
6. **Given** an organiser applies `filter[declined]=true` together with another filter (e.g., `filter[pack_id]=xxx`), **When** the request is processed, **Then** only partnerships matching ALL filter criteria are returned (AND logic)

---

### User Story 2 - Control Declined Partnerships in Bulk Email Sending (Priority: P2)

When organisers send bulk emails to partnership contacts, they typically want to communicate only with active partners, not with those who have already declined. The email endpoint shares the same filtering logic as the list endpoint. Adding `filter[declined]` to the email endpoint ensures organisers can explicitly control whether declined partnerships are included as email recipients, preventing accidental communication with companies that have already refused participation.

**Why this priority**: Extending the filter to the email endpoint is required by the feature description and is consistent with the design contract established in spec `014-email-partnerships`. It is slightly lower priority than P1 because the email endpoint behaviour is secondary to the list endpoint, and the safest default (exclude declined) already protects against accidental sends.

**Independent Test**: Can be fully tested independently by calling the POST email partnerships endpoint with `filter[declined]=true` and verifying that declined partnerships are included as recipients, and calling it without the filter and verifying that declined partnerships are excluded from the recipient list. Delivers standalone value as protection against unwanted communication with declined partners.

**Acceptance Scenarios**:

1. **Given** an event has active and declined partnerships, **When** an organiser sends an email without `filter[declined]`, **Then** only non-declined partnerships receive the email
2. **Given** an event has active and declined partnerships, **When** an organiser sends an email with `filter[declined]=false`, **Then** only non-declined partnerships receive the email
3. **Given** an event has active and declined partnerships, **When** an organiser sends an email with `filter[declined]=true`, **Then** both active and declined partnerships with contact emails are included as recipients
4. **Given** an organiser sends an email with `filter[declined]=true` combined with another filter, **When** the request is processed, **Then** only partnerships matching ALL filter criteria (including declined status) are included as email recipients
5. **Given** all partnerships matching the applied filters are declined and `filter[declined]` is not set to `true`, **When** the email request is processed, **Then** the system returns HTTP 204 No Content (no recipients after exclusion)

---

### Edge Cases

- What happens when `filter[declined]` is provided with an invalid value (e.g., a non-boolean string)? The system should return HTTP 400 Bad Request with a clear error message.
- What happens when `filter[declined]=true` is combined with other filters that result in zero matches? Return an empty list with HTTP 200 for the list endpoint, HTTP 204 for the email endpoint.
- What happens when the event has no declined partnerships and `filter[declined]=true` is set? Return results as if the filter were absent (all active partnerships), since no declined ones exist to include.
- What happens when `filter[declined]=true` is set and all partnerships on the event are declined? Only declined partnerships are returned (no active ones to combine with).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST add a new optional query parameter `filter[declined]` to the existing GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` endpoint that accepts a boolean value (`true` or `false`)
- **FR-002**: System MUST exclude partnerships where `declinedAt IS NOT NULL` by default when `filter[declined]` is not provided or is explicitly set to `false`
- **FR-003**: System MUST include partnerships where `declinedAt IS NOT NULL` in results when `filter[declined]=true` is provided; the filter MUST NOT use the `suggestionDeclinedAt` column
- **FR-004**: System MUST apply the `filter[declined]` filter using AND logic with all other active filters (`filter[pack_id]`, `filter[validated]`, `filter[paid]`, `filter[organiser]`, `filter[agreement-generated]`, `filter[agreement-signed]`, etc.)
- **FR-005**: System MUST return HTTP 400 Bad Request when `filter[declined]` is provided with a value that cannot be interpreted as a boolean
- **FR-006**: System MUST add the same `filter[declined]` query parameter to the POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` endpoint
- **FR-007**: System MUST apply the same `filter[declined]` semantics in the email endpoint as in the list endpoint (default excludes declined; `true` includes them)
- **FR-008**: System MUST apply `filter[declined]` using AND logic with all other filters in the email endpoint
- **FR-009**: System MUST return HTTP 200 with an empty list when the partnership list query matches zero partnerships after applying the declined filter
- **FR-010**: System MUST return HTTP 204 No Content when the email endpoint query matches zero recipients after applying the declined filter
- **FR-011**: System MUST include a `filter[declined]` entry with `type: "boolean"` in the `filters` metadata array of every partnership list response, consistent with the metadata contract established in spec `015-filter-partnerships-organiser`

### Key Entities

- **Partnership**: Represents a sponsorship relationship between a company and an event. Relevant column: `declinedAt` (nullable datetime). A partnership is considered declined when `declinedAt IS NOT NULL`. The `filter[declined]` filter operates exclusively on this column. `suggestionDeclinedAt` (nullable datetime on the same table) tracks whether a pack suggestion was declined and must NOT be used by this filter.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Organisers can retrieve a list that includes declined partnerships by passing a single additional filter parameter, without any change to the default behaviour for existing consumers of the partnership list endpoint
- **SC-002**: The default behaviour change (excluding declined partnerships) is intentional and explicitly documented; existing callers are expected to adapt by passing `filter[declined]=true` if they need to retain the previous behaviour
- **SC-003**: The `filter[declined]` parameter is accepted, with identical semantics, on both the partnership list endpoint and the email endpoint
- **SC-004**: Invalid filter values result in an immediate, descriptive error response so organisers can self-correct without contacting support
- **SC-005**: The `filters` metadata in every partnership list response includes the `filter[declined]` entry with `type: "boolean"`, keeping the discoverability contract complete
