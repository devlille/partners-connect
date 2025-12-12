import { describe, it, expect } from "vitest";
import type { IntegrationProvider, IntegrationUsage, IntegrationConfig } from "~/types/integration";
import {
  createMailjetConfig,
  createQontoConfig,
  createBilletwebConfig,
  createOpenPlannerConfig,
  createSlackConfig,
  createWebhookConfig,
} from "../helpers/integrationFactory";

describe("Integration Types", () => {
  describe("IntegrationProvider", () => {
    it("should accept valid provider types", () => {
      const providers: IntegrationProvider[] = [
        "QONTO",
        "MAILJET",
        "BILLETWEB",
        "OPENPLANNER",
        "SLACK",
        "WEBHOOK",
      ];

      providers.forEach((provider) => {
        expect(provider).toBeDefined();
        expect(typeof provider).toBe("string");
      });
    });
  });

  describe("IntegrationUsage", () => {
    it("should accept valid usage types", () => {
      const usages: IntegrationUsage[] = [
        "NOTIFICATION",
        "BILLING",
        "MAILING",
        "TICKETING",
        "WEBHOOK",
        "AGENDA",
      ];

      usages.forEach((usage) => {
        expect(usage).toBeDefined();
        expect(typeof usage).toBe("string");
      });
    });
  });

  describe("QontoConfig", () => {
    it("should have required fields", () => {
      const config = createQontoConfig();

      expect(config.api_key).toBeDefined();
      expect(config.secret).toBeDefined();
      expect(config.sandbox_token).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createQontoConfig();

      expect(typeof config.api_key).toBe("string");
      expect(typeof config.secret).toBe("string");
      expect(typeof config.sandbox_token).toBe("string");
    });
  });

  describe("MailjetConfig", () => {
    it("should have required fields", () => {
      const config = createMailjetConfig();

      expect(config.api_key).toBeDefined();
      expect(config.secret).toBeDefined();
      expect(config.sender_email).toBeDefined();
      expect(config.sender_name).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createMailjetConfig();

      expect(typeof config.api_key).toBe("string");
      expect(typeof config.secret).toBe("string");
      expect(typeof config.sender_email).toBe("string");
      expect(typeof config.sender_name).toBe("string");
    });
  });

  describe("BilletwebConfig", () => {
    it("should have required fields", () => {
      const config = createBilletwebConfig();

      expect(config.basic).toBeDefined();
      expect(config.event_id).toBeDefined();
      expect(config.rate_id).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createBilletwebConfig();

      expect(typeof config.basic).toBe("string");
      expect(typeof config.event_id).toBe("string");
      expect(typeof config.rate_id).toBe("string");
    });
  });

  describe("OpenPlannerConfig", () => {
    it("should have required fields", () => {
      const config = createOpenPlannerConfig();

      expect(config.api_key).toBeDefined();
      expect(config.event_id).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createOpenPlannerConfig();

      expect(typeof config.api_key).toBe("string");
      expect(typeof config.event_id).toBe("string");
    });
  });

  describe("SlackConfig", () => {
    it("should have required fields", () => {
      const config = createSlackConfig();

      expect(config.token).toBeDefined();
      expect(config.channel).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createSlackConfig();

      expect(typeof config.token).toBe("string");
      expect(typeof config.channel).toBe("string");
    });

    it("should ensure channel starts with #", () => {
      const config = createSlackConfig({ channel: "#test" });

      expect(config.channel.startsWith("#")).toBe(true);
    });
  });

  describe("WebhookConfig", () => {
    it("should have required fields", () => {
      const config = createWebhookConfig();

      expect(config.url).toBeDefined();
      expect(config.secret).toBeDefined();
      expect(config.type).toBeDefined();
    });

    it("should validate field types", () => {
      const config = createWebhookConfig();

      expect(typeof config.url).toBe("string");
      expect(typeof config.secret).toBe("string");
      expect(typeof config.type).toBe("string");
    });

    it("should accept valid webhook types", () => {
      const allConfig = createWebhookConfig({ type: "ALL" });
      const partnershipConfig = createWebhookConfig({ type: "PARTNERSHIP" });

      expect(allConfig.type).toBe("ALL");
      expect(partnershipConfig.type).toBe("PARTNERSHIP");
    });

    it("should validate URL format", () => {
      const config = createWebhookConfig({ url: "https://example.com/webhook" });

      expect(config.url.startsWith("https://")).toBe(true);
    });

    it("should validate secret minimum length", () => {
      const config = createWebhookConfig();

      expect(config.secret.length).toBeGreaterThanOrEqual(16);
    });
  });

  describe("IntegrationConfig", () => {
    it("should accept valid integration config", () => {
      const config: IntegrationConfig = {
        provider: "QONTO",
        usage: "BILLING",
        config: {
          api_key: "test-key",
          secret: "test-secret",
        },
      };

      expect(config.provider).toBe("QONTO");
      expect(config.usage).toBe("BILLING");
      expect(config.config).toBeDefined();
      expect(typeof config.config).toBe("object");
    });

    it("should accept different provider/usage combinations", () => {
      const configs: IntegrationConfig[] = [
        {
          provider: "QONTO",
          usage: "BILLING",
          config: {},
        },
        {
          provider: "MAILJET",
          usage: "NOTIFICATION",
          config: {},
        },
        {
          provider: "BILLETWEB",
          usage: "TICKETING",
          config: {},
        },
        {
          provider: "OPENPLANNER",
          usage: "AGENDA",
          config: {},
        },
        {
          provider: "SLACK",
          usage: "NOTIFICATION",
          config: {},
        },
        {
          provider: "WEBHOOK",
          usage: "WEBHOOK",
          config: {},
        },
      ];

      configs.forEach((config) => {
        expect(config.provider).toBeDefined();
        expect(config.usage).toBeDefined();
        expect(config.config).toBeDefined();
      });
    });
  });

  describe("Type Safety", () => {
    it("should enforce QontoConfig structure", () => {
      const config = createQontoConfig({
        api_key: "key",
        secret: "secret",
        sandbox_token: "token",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(3);
      expect(keys).toContain("api_key");
      expect(keys).toContain("secret");
      expect(keys).toContain("sandbox_token");
    });

    it("should enforce MailjetConfig structure", () => {
      const config = createMailjetConfig({
        api_key: "key",
        secret: "secret",
        sender_email: "test@example.com",
        sender_name: "Test",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(4);
      expect(keys).toContain("api_key");
      expect(keys).toContain("secret");
      expect(keys).toContain("sender_email");
      expect(keys).toContain("sender_name");
    });

    it("should enforce BilletwebConfig structure", () => {
      const config = createBilletwebConfig({
        basic: "auth",
        event_id: "123",
        rate_id: "456",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(3);
      expect(keys).toContain("basic");
      expect(keys).toContain("event_id");
      expect(keys).toContain("rate_id");
    });

    it("should enforce OpenPlannerConfig structure", () => {
      const config = createOpenPlannerConfig({
        api_key: "key",
        event_id: "event-123",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(2);
      expect(keys).toContain("api_key");
      expect(keys).toContain("event_id");
    });

    it("should enforce SlackConfig structure", () => {
      const config = createSlackConfig({
        token: "xoxb-token",
        channel: "#channel",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(2);
      expect(keys).toContain("token");
      expect(keys).toContain("channel");
    });

    it("should enforce WebhookConfig structure", () => {
      const config = createWebhookConfig({
        url: "https://example.com/webhook",
        secret: "secret1234567890",
        type: "ALL",
      });

      const keys = Object.keys(config);
      expect(keys).toHaveLength(3);
      expect(keys).toContain("url");
      expect(keys).toContain("secret");
      expect(keys).toContain("type");
    });
  });
});
