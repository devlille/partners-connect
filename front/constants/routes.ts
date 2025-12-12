/**
 * Routes de l'application
 * Centralise toutes les routes pour éviter les erreurs de typage et faciliter la maintenance
 */

export const ROUTES = {
  // Auth
  LOGIN: "/login",
  AUTH_CALLBACK: "/auth/callback",

  // Home
  HOME: "/",
  HALL: "/hall",

  // Organizations
  ORGS: "/orgs",
  ORG_CREATE: "/orgs/create",
  ORG_DETAIL: (slug: string) => `/orgs/${slug}`,
  ORG_USERS: (slug: string) => `/orgs/${slug}/users`,

  // Events
  EVENTS: (orgSlug: string) => `/orgs/${orgSlug}/events`,
  EVENT_CREATE: (orgSlug: string) => `/orgs/${orgSlug}/events/create`,
  EVENT_DETAIL: (orgSlug: string, eventSlug: string) => `/orgs/${orgSlug}/events/${eventSlug}`,

  // Packs
  PACKS: (orgSlug: string, eventSlug: string) => `/orgs/${orgSlug}/events/${eventSlug}/packs`,
  PACK_CREATE: (orgSlug: string, eventSlug: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/packs/create`,
  PACK_DETAIL: (orgSlug: string, eventSlug: string, packId: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/packs/${packId}`,

  // Options
  OPTIONS: (orgSlug: string, eventSlug: string) => `/orgs/${orgSlug}/events/${eventSlug}/options`,
  OPTION_CREATE: (orgSlug: string, eventSlug: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/options/create`,
  OPTION_DETAIL: (orgSlug: string, eventSlug: string, optionId: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/options/${optionId}`,

  // Sponsors
  SPONSORS: (orgSlug: string, eventSlug: string) => `/orgs/${orgSlug}/events/${eventSlug}/sponsors`,
  SPONSOR_CREATE: (orgSlug: string, eventSlug: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/sponsors/create`,
  SPONSOR_DETAIL: (orgSlug: string, eventSlug: string, sponsorId: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}`,
  SPONSOR_JOB_OFFERS: (orgSlug: string, eventSlug: string, sponsorId: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/job-offers`,
  SPONSOR_EXTERNAL_LINKS: (orgSlug: string, eventSlug: string, sponsorId: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/external-links`,

  // Providers
  PROVIDERS: (orgSlug: string, eventSlug: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/providers`,

  // External Links
  EXTERNAL_LINKS: (orgSlug: string, eventSlug: string) =>
    `/orgs/${orgSlug}/events/${eventSlug}/external-links`,

  // Companies
  COMPANIES: (companyId: string) => `/companies/${companyId}`,
  COMPANY_JOB_OFFERS: (companyId: string) => `/companies/${companyId}/job-offers`,

  // Settings
  SETTINGS: "/settings",
} as const;
