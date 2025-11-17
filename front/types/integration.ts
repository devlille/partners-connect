export type IntegrationProvider = 'QONTO' | 'MAILJET' | 'BILLETWEB';
export type IntegrationUsage = 'NOTIFICATION' | 'BILLING' | 'MAILING' | 'TICKETING' | 'WEBHOOK' | 'AGENDA';

export interface IntegrationConfig {
  provider: IntegrationProvider;
  usage: IntegrationUsage;
  config: Record<string, unknown>;
}

export interface QontoConfig {
  api_key: string;
  secret: string;
  sandbox_token: string;
}

export interface MailjetConfig {
  api_key: string;
  secret: string;
  from_email: string;
  from_name: string;
}

export interface BilletwebConfig {
  basic: string;
  event_id: string;
  rate_id: string;
}
