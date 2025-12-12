export type IntegrationProvider =
  | "QONTO"
  | "MAILJET"
  | "BILLETWEB"
  | "OPENPLANNER"
  | "SLACK"
  | "WEBHOOK";
export type IntegrationUsage =
  | "NOTIFICATION"
  | "BILLING"
  | "MAILING"
  | "TICKETING"
  | "WEBHOOK"
  | "AGENDA";

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
  sender_email: string;
  sender_name: string;
}

export interface BilletwebConfig {
  basic: string;
  event_id: string;
  rate_id: string;
}

export interface OpenPlannerConfig {
  api_key: string;
  event_id: string;
}

export interface SlackConfig {
  token: string;
  channel: string;
}

export interface WebhookConfig {
  url: string;
  secret: string;
  type: string;
}
