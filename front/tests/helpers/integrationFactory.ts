import type { MailjetConfig, QontoConfig, BilletwebConfig } from '~/types/integration';

/**
 * Factory pour créer une configuration Mailjet de test
 */
export function createMailjetConfig(overrides?: Partial<MailjetConfig>): MailjetConfig {
  return {
    api_key: 'test-api-key',
    secret: 'test-secret',
    from_email: 'test@example.com',
    from_name: 'Test Sender',
    ...overrides
  };
}

/**
 * Factory pour créer une configuration Qonto de test
 */
export function createQontoConfig(overrides?: Partial<QontoConfig>): QontoConfig {
  return {
    api_key: 'test-api-key',
    secret: 'test-secret',
    sandbox_token: 'test-token',
    ...overrides
  };
}

/**
 * Factory pour créer une configuration Billetweb de test
 */
export function createBilletwebConfig(overrides?: Partial<BilletwebConfig>): BilletwebConfig {
  return {
    basic: 'basic-auth-token',
    event_id: '12345',
    rate_id: '67890',
    ...overrides
  };
}
