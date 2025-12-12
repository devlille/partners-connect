import type { IntegrationSchemaProvider, IntegrationSchemaUsage } from "./api";

/**
 * Provider display metadata
 */
export interface ProviderMetadata {
  /** Provider type identifier */
  type: IntegrationSchemaProvider;

  /** Human-readable provider name */
  name: string;

  /** Icon class (heroicons) */
  icon: string;

  /** Supported usage categories */
  supportedUsages: IntegrationSchemaUsage[];

  /** Form component name */
  formComponent: string;
}

/**
 * Provider metadata registry
 * Maps each integration provider to its display metadata and configuration
 */
export const PROVIDERS: Record<IntegrationSchemaProvider, ProviderMetadata> = {
  qonto: {
    type: "qonto",
    name: "Qonto",
    icon: "i-heroicons-currency-euro",
    supportedUsages: ["billing"],
    formComponent: "QontoForm",
  },
  mailjet: {
    type: "mailjet",
    name: "Mailjet",
    icon: "i-heroicons-envelope",
    supportedUsages: ["notification"],
    formComponent: "MailjetForm",
  },
  billetweb: {
    type: "billetweb",
    name: "BilletWeb",
    icon: "i-heroicons-ticket",
    supportedUsages: ["ticketing"],
    formComponent: "BilletwebForm",
  },
  openplanner: {
    type: "openplanner",
    name: "OpenPlanner",
    icon: "i-heroicons-calendar",
    supportedUsages: ["agenda"],
    formComponent: "OpenPlannerForm",
  },
  slack: {
    type: "slack",
    name: "Slack",
    icon: "i-heroicons-chat-bubble-left-right",
    supportedUsages: ["notification"],
    formComponent: "SlackForm",
  },
  webhook: {
    type: "webhook",
    name: "Webhook",
    icon: "i-heroicons-arrow-path",
    supportedUsages: ["webhook"],
    formComponent: "WebhookForm",
  },
};

/**
 * Get all available provider types
 */
export const ALL_PROVIDERS = Object.keys(PROVIDERS) as IntegrationSchemaProvider[];
