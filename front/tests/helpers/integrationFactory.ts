import type {
  MailjetConfig,
  QontoConfig,
  BilletwebConfig,
  OpenPlannerConfig,
  SlackConfig,
  WebhookConfig,
} from "~/types/integration";

/**
 * Factory pour créer une configuration Mailjet de test
 */
export function createMailjetConfig(overrides?: Partial<MailjetConfig>): MailjetConfig {
  return {
    api_key: "test-api-key",
    secret: "test-secret",
    sender_email: "test@example.com",
    sender_name: "Test Sender",
    ...overrides,
  };
}

/**
 * Factory pour créer une configuration Qonto de test
 */
export function createQontoConfig(overrides?: Partial<QontoConfig>): QontoConfig {
  return {
    api_key: "test-api-key",
    secret: "test-secret",
    sandbox_token: "test-token",
    ...overrides,
  };
}

/**
 * Factory pour créer une configuration Billetweb de test
 */
export function createBilletwebConfig(overrides?: Partial<BilletwebConfig>): BilletwebConfig {
  return {
    basic: "basic-auth-token",
    event_id: "12345",
    rate_id: "67890",
    ...overrides,
  };
}

/**
 * Factory pour créer une configuration OpenPlanner de test
 */
export function createOpenPlannerConfig(overrides?: Partial<OpenPlannerConfig>): OpenPlannerConfig {
  return {
    api_key: "test-api-key",
    event_id: "event-123",
    ...overrides,
  };
}

/**
 * Factory pour créer une configuration Slack de test
 */
export function createSlackConfig(overrides?: Partial<SlackConfig>): SlackConfig {
  return {
    token: "xoxb-test-token",
    channel: "#test-channel",
    ...overrides,
  };
}

/**
 * Factory pour créer une configuration Webhook de test
 */
export function createWebhookConfig(overrides?: Partial<WebhookConfig>): WebhookConfig {
  return {
    url: "https://example.com/webhook",
    secret: "test-secret-1234567890",
    type: "ALL",
    ...overrides,
  };
}
