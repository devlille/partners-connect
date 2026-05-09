import { customFetch } from '../custom-instance';
import type { PartnershipItemSchema } from './api';

export interface JobOfferStatsSchema {
  total: number;
  validated: number;
}

export interface QandaStatsSchema {
  questions: number;
  answers: number;
}

export interface PartnerStatsSchema {
  partnership: PartnershipItemSchema;
  job_offers: JobOfferStatsSchema;
  activities: number;
  qanda: QandaStatsSchema;
  tickets: number;
  social_links: number;
  communication_plan: number;
  speakers: number;
}

export interface EventStatsSchema {
  partners: PartnerStatsSchema[];
}

/**
 * Per-partner engagement stats for an event.
 * @summary Get event stats for organisers
 */
export const getEventStats = (orgSlug: string, eventSlug: string) => {
  return customFetch<EventStatsSchema>({
    url: `/orgs/${orgSlug}/events/${eventSlug}/stats`,
    method: 'GET',
  });
};
