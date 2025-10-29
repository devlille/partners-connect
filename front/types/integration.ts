export type IntegrationProvider = 'QONTO' | 'MAILJET' | 'BILLETWEB';
export type IntegrationUsage = 'BILLING' | 'MAILING' | 'TICKETING';

export interface IntegrationConfig {
  provider: IntegrationProvider;
  usage: IntegrationUsage;
  config: Record<string, unknown>;
}

export interface QontoConfig {
  organization_slug: string;
  secret_key: string;
}

export interface MailjetConfig {
  api_key: string;
  api_secret: string;
  from_email: string;
  from_name: string;
}

export interface BilletwebConfig {
  api_key: string;
  event_id: string;
}
