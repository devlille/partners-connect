import { describe, it, expect } from 'vitest';
import type { QontoConfig, MailjetConfig, BilletwebConfig } from '~/types/integration';

describe('Integrations Page - Helper Functions', () => {
  describe('Qonto Configuration Validation', () => {
    function isValidQontoConfig(config: Partial<QontoConfig>): boolean {
      return !!(
        config.organization_slug &&
        config.organization_slug.trim() !== '' &&
        config.secret_key &&
        config.secret_key.trim() !== ''
      );
    }

    it('should validate complete Qonto config', () => {
      const config: QontoConfig = {
        organization_slug: 'my-org',
        secret_key: 'sk_live_1234567890'
      };

      expect(isValidQontoConfig(config)).toBe(true);
    });

    it('should invalidate Qonto config without organization_slug', () => {
      const config: Partial<QontoConfig> = {
        secret_key: 'sk_live_1234567890'
      };

      expect(isValidQontoConfig(config)).toBe(false);
    });

    it('should invalidate Qonto config without secret_key', () => {
      const config: Partial<QontoConfig> = {
        organization_slug: 'my-org'
      };

      expect(isValidQontoConfig(config)).toBe(false);
    });

    it('should invalidate Qonto config with empty strings', () => {
      const config: QontoConfig = {
        organization_slug: '',
        secret_key: 'sk_live_1234567890'
      };

      expect(isValidQontoConfig(config)).toBe(false);
    });

    it('should invalidate Qonto config with whitespace-only strings', () => {
      const config: QontoConfig = {
        organization_slug: '   ',
        secret_key: 'sk_live_1234567890'
      };

      expect(isValidQontoConfig(config)).toBe(false);
    });
  });

  describe('Mailjet Configuration Validation', () => {
    function isValidMailjetConfig(config: Partial<MailjetConfig>): boolean {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      return !!(
        config.api_key &&
        config.api_key.trim() !== '' &&
        config.api_secret &&
        config.api_secret.trim() !== '' &&
        config.from_email &&
        emailRegex.test(config.from_email) &&
        config.from_name &&
        config.from_name.trim() !== ''
      );
    }

    it('should validate complete Mailjet config', () => {
      const config: MailjetConfig = {
        api_key: 'api_key_123',
        api_secret: 'api_secret_456',
        from_email: 'contact@example.com',
        from_name: 'My Event'
      };

      expect(isValidMailjetConfig(config)).toBe(true);
    });

    it('should invalidate Mailjet config without api_key', () => {
      const config: Partial<MailjetConfig> = {
        api_secret: 'api_secret_456',
        from_email: 'contact@example.com',
        from_name: 'My Event'
      };

      expect(isValidMailjetConfig(config)).toBe(false);
    });

    it('should invalidate Mailjet config without api_secret', () => {
      const config: Partial<MailjetConfig> = {
        api_key: 'api_key_123',
        from_email: 'contact@example.com',
        from_name: 'My Event'
      };

      expect(isValidMailjetConfig(config)).toBe(false);
    });

    it('should invalidate Mailjet config with invalid email', () => {
      const config: MailjetConfig = {
        api_key: 'api_key_123',
        api_secret: 'api_secret_456',
        from_email: 'invalid-email',
        from_name: 'My Event'
      };

      expect(isValidMailjetConfig(config)).toBe(false);
    });

    it('should invalidate Mailjet config without from_name', () => {
      const config: Partial<MailjetConfig> = {
        api_key: 'api_key_123',
        api_secret: 'api_secret_456',
        from_email: 'contact@example.com'
      };

      expect(isValidMailjetConfig(config)).toBe(false);
    });

    it('should validate Mailjet config with various valid email formats', () => {
      const validEmails = [
        'user@example.com',
        'user.name@example.com',
        'user+tag@example.co.uk',
        'user_name@example-domain.com'
      ];

      validEmails.forEach(email => {
        const config: MailjetConfig = {
          api_key: 'api_key_123',
          api_secret: 'api_secret_456',
          from_email: email,
          from_name: 'My Event'
        };

        expect(isValidMailjetConfig(config)).toBe(true);
      });
    });
  });

  describe('Billetweb Configuration Validation', () => {
    function isValidBilletwebConfig(config: Partial<BilletwebConfig>): boolean {
      return !!(
        config.api_key &&
        config.api_key.trim() !== '' &&
        config.event_id &&
        config.event_id.trim() !== ''
      );
    }

    it('should validate complete Billetweb config', () => {
      const config: BilletwebConfig = {
        api_key: 'bw_api_key_123',
        event_id: '12345'
      };

      expect(isValidBilletwebConfig(config)).toBe(true);
    });

    it('should invalidate Billetweb config without api_key', () => {
      const config: Partial<BilletwebConfig> = {
        event_id: '12345'
      };

      expect(isValidBilletwebConfig(config)).toBe(false);
    });

    it('should invalidate Billetweb config without event_id', () => {
      const config: Partial<BilletwebConfig> = {
        api_key: 'bw_api_key_123'
      };

      expect(isValidBilletwebConfig(config)).toBe(false);
    });

    it('should invalidate Billetweb config with empty strings', () => {
      const config: BilletwebConfig = {
        api_key: '',
        event_id: '12345'
      };

      expect(isValidBilletwebConfig(config)).toBe(false);
    });

    it('should validate Billetweb config with numeric event_id', () => {
      const config: BilletwebConfig = {
        api_key: 'bw_api_key_123',
        event_id: '9876543210'
      };

      expect(isValidBilletwebConfig(config)).toBe(true);
    });
  });

  describe('Integration Provider Detection', () => {
    type IntegrationProvider = 'QONTO' | 'MAILJET' | 'BILLETWEB';

    function getProviderDisplayName(provider: IntegrationProvider): string {
      const names: Record<IntegrationProvider, string> = {
        QONTO: 'Qonto',
        MAILJET: 'Mailjet',
        BILLETWEB: 'Billetweb'
      };
      return names[provider];
    }

    it('should return correct display names', () => {
      expect(getProviderDisplayName('QONTO')).toBe('Qonto');
      expect(getProviderDisplayName('MAILJET')).toBe('Mailjet');
      expect(getProviderDisplayName('BILLETWEB')).toBe('Billetweb');
    });
  });

  describe('Integration Usage Detection', () => {
    type IntegrationUsage = 'BILLING' | 'MAILING' | 'TICKETING';

    function getUsageDisplayName(usage: IntegrationUsage): string {
      const names: Record<IntegrationUsage, string> = {
        BILLING: 'Facturation',
        MAILING: 'Envoi d\'emails',
        TICKETING: 'Billetterie'
      };
      return names[usage];
    }

    it('should return correct usage display names', () => {
      expect(getUsageDisplayName('BILLING')).toBe('Facturation');
      expect(getUsageDisplayName('MAILING')).toBe('Envoi d\'emails');
      expect(getUsageDisplayName('TICKETING')).toBe('Billetterie');
    });
  });

  describe('API Key Format Validation', () => {
    function isValidApiKeyFormat(apiKey: string, prefix?: string): boolean {
      if (!apiKey || apiKey.trim() === '') return false;
      if (prefix && !apiKey.startsWith(prefix)) return false;
      return apiKey.length >= 10;
    }

    it('should validate API keys with minimum length', () => {
      expect(isValidApiKeyFormat('1234567890')).toBe(true);
      expect(isValidApiKeyFormat('sk_live_1234567890')).toBe(true);
    });

    it('should invalidate short API keys', () => {
      expect(isValidApiKeyFormat('short')).toBe(false);
      expect(isValidApiKeyFormat('123')).toBe(false);
    });

    it('should validate API keys with specific prefix', () => {
      expect(isValidApiKeyFormat('sk_live_1234567890', 'sk_live_')).toBe(true);
      expect(isValidApiKeyFormat('sk_test_1234567890', 'sk_test_')).toBe(true);
    });

    it('should invalidate API keys with wrong prefix', () => {
      expect(isValidApiKeyFormat('pk_live_1234567890', 'sk_live_')).toBe(false);
      expect(isValidApiKeyFormat('other_1234567890', 'sk_')).toBe(false);
    });

    it('should invalidate empty or whitespace-only API keys', () => {
      expect(isValidApiKeyFormat('')).toBe(false);
      expect(isValidApiKeyFormat('   ')).toBe(false);
    });
  });

  describe('Configuration Sanitization', () => {
    function sanitizeConfig<T extends Record<string, any>>(config: T): T {
      const sanitized = {} as T;
      for (const key in config) {
        if (typeof config[key] === 'string') {
          sanitized[key] = config[key].trim() as T[Extract<keyof T, string>];
        } else {
          sanitized[key] = config[key];
        }
      }
      return sanitized;
    }

    it('should trim whitespace from string values', () => {
      const config = {
        api_key: '  my-api-key  ',
        event_id: '  12345  '
      };

      const sanitized = sanitizeConfig(config);

      expect(sanitized.api_key).toBe('my-api-key');
      expect(sanitized.event_id).toBe('12345');
    });

    it('should preserve non-string values', () => {
      const config = {
        api_key: 'my-api-key',
        max_retries: 3,
        enabled: true
      };

      const sanitized = sanitizeConfig(config);

      expect(sanitized.max_retries).toBe(3);
      expect(sanitized.enabled).toBe(true);
    });
  });

  describe('Error Message Extraction', () => {
    function extractErrorMessage(error: any): string {
      if (error.response?.data?.message) {
        return error.response.data.message;
      }
      if (error.response?.status === 401) {
        return 'Non autorisé. Veuillez vérifier vos identifiants.';
      }
      if (error.response?.status === 403) {
        return 'Accès refusé.';
      }
      if (error.response?.status === 404) {
        return 'Ressource introuvable.';
      }
      if (error.response?.status === 500) {
        return 'Erreur serveur. Veuillez réessayer plus tard.';
      }
      return 'Une erreur est survenue.';
    }

    it('should extract server error message', () => {
      const error = {
        response: {
          data: {
            message: 'Invalid API key'
          }
        }
      };

      expect(extractErrorMessage(error)).toBe('Invalid API key');
    });

    it('should return appropriate message for 401 error', () => {
      const error = {
        response: {
          status: 401
        }
      };

      expect(extractErrorMessage(error)).toBe('Non autorisé. Veuillez vérifier vos identifiants.');
    });

    it('should return appropriate message for 403 error', () => {
      const error = {
        response: {
          status: 403
        }
      };

      expect(extractErrorMessage(error)).toBe('Accès refusé.');
    });

    it('should return appropriate message for 404 error', () => {
      const error = {
        response: {
          status: 404
        }
      };

      expect(extractErrorMessage(error)).toBe('Ressource introuvable.');
    });

    it('should return generic message for unknown errors', () => {
      const error = {};

      expect(extractErrorMessage(error)).toBe('Une erreur est survenue.');
    });
  });
});
