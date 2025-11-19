import { describe, it, expect } from 'vitest';
import type { IntegrationProvider, IntegrationUsage, IntegrationConfig } from "~/types/integration";
import { createMailjetConfig, createQontoConfig, createBilletwebConfig } from '../helpers/integrationFactory';

describe('Integration Types', () => {
  describe('IntegrationProvider', () => {
    it('should accept valid provider types', () => {
      const providers: IntegrationProvider[] = ['QONTO', 'MAILJET', 'BILLETWEB'];

      providers.forEach(provider => {
        expect(provider).toBeDefined();
        expect(typeof provider).toBe('string');
      });
    });
  });

  describe('IntegrationUsage', () => {
    it('should accept valid usage types', () => {
      const usages: IntegrationUsage[] = [
        'NOTIFICATION',
        'BILLING',
        'MAILING',
        'TICKETING',
        'WEBHOOK',
        'AGENDA'
      ];

      usages.forEach(usage => {
        expect(usage).toBeDefined();
        expect(typeof usage).toBe('string');
      });
    });
  });

  describe('QontoConfig', () => {
    it('should have required fields', () => {
      const config = createQontoConfig();

      expect(config.api_key).toBeDefined();
      expect(config.secret).toBeDefined();
      expect(config.sandbox_token).toBeDefined();
    });

    it('should validate field types', () => {
      const config = createQontoConfig();

      expect(typeof config.api_key).toBe('string');
      expect(typeof config.secret).toBe('string');
      expect(typeof config.sandbox_token).toBe('string');
    });
  });

  describe('MailjetConfig', () => {
    it('should have required fields', () => {
      const config = createMailjetConfig();

      expect(config.api_key).toBeDefined();
      expect(config.secret).toBeDefined();
      expect(config.from_email).toBeDefined();
      expect(config.from_name).toBeDefined();
    });

    it('should validate field types', () => {
      const config = createMailjetConfig();

      expect(typeof config.api_key).toBe('string');
      expect(typeof config.secret).toBe('string');
      expect(typeof config.from_email).toBe('string');
      expect(typeof config.from_name).toBe('string');
    });
  });

  describe('BilletwebConfig', () => {
    it('should have required fields', () => {
      const config = createBilletwebConfig();

      expect(config.basic).toBeDefined();
      expect(config.event_id).toBeDefined();
      expect(config.rate_id).toBeDefined();
    });

    it('should validate field types', () => {
      const config = createBilletwebConfig();

      expect(typeof config.basic).toBe('string');
      expect(typeof config.event_id).toBe('string');
      expect(typeof config.rate_id).toBe('string');
    });
  });

  describe('IntegrationConfig', () => {
    it('should accept valid integration config', () => {
      const config: IntegrationConfig = {
        provider: 'QONTO',
        usage: 'BILLING',
        config: {
          api_key: 'test-key',
          secret: 'test-secret'
        }
      };

      expect(config.provider).toBe('QONTO');
      expect(config.usage).toBe('BILLING');
      expect(config.config).toBeDefined();
      expect(typeof config.config).toBe('object');
    });

    it('should accept different provider/usage combinations', () => {
      const configs: IntegrationConfig[] = [
        {
          provider: 'QONTO',
          usage: 'BILLING',
          config: {}
        },
        {
          provider: 'MAILJET',
          usage: 'NOTIFICATION',
          config: {}
        },
        {
          provider: 'BILLETWEB',
          usage: 'TICKETING',
          config: {}
        }
      ];

      configs.forEach(config => {
        expect(config.provider).toBeDefined();
        expect(config.usage).toBeDefined();
        expect(config.config).toBeDefined();
      });
    });
  });

  describe('Type Safety', () => {
    it('should enforce QontoConfig structure', () => {
      const config = createQontoConfig({
        api_key: 'key',
        secret: 'secret',
        sandbox_token: 'token'
      });

      // Should have exactly these fields
      const keys = Object.keys(config);
      expect(keys).toHaveLength(3);
      expect(keys).toContain('api_key');
      expect(keys).toContain('secret');
      expect(keys).toContain('sandbox_token');
    });

    it('should enforce MailjetConfig structure', () => {
      const config = createMailjetConfig({
        api_key: 'key',
        secret: 'secret',
        from_email: 'test@example.com',
        from_name: 'Test'
      });

      // Should have exactly these fields
      const keys = Object.keys(config);
      expect(keys).toHaveLength(4);
      expect(keys).toContain('api_key');
      expect(keys).toContain('secret');
      expect(keys).toContain('from_email');
      expect(keys).toContain('from_name');
    });

    it('should enforce BilletwebConfig structure', () => {
      const config = createBilletwebConfig({
        basic: 'auth',
        event_id: '123',
        rate_id: '456'
      });

      // Should have exactly these fields
      const keys = Object.keys(config);
      expect(keys).toHaveLength(3);
      expect(keys).toContain('basic');
      expect(keys).toContain('event_id');
      expect(keys).toContain('rate_id');
    });
  });
});
