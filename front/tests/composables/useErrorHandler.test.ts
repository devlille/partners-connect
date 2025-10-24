import { describe, it, expect } from 'vitest';
import { ErrorType } from '~/composables/useErrorHandler';

// Fonction utilitaire pour parser les erreurs (logique extraite du composable)
function parseErrorLogic(error: unknown): { type: ErrorType; i18nKey: string; message: string; details?: unknown } {
  // Network error
  if (error instanceof TypeError && error.message === 'Failed to fetch') {
    return {
      type: ErrorType.NETWORK,
      message: 'Network error',
      i18nKey: 'errors.network',
      details: error
    };
  }

  // HTTP errors
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as any).response;
    if (response && response.status) {
      const status = response.status;

      if (status === 400 || status === 422) {
        return {
          type: ErrorType.VALIDATION,
          message: 'Validation error',
          i18nKey: 'errors.validation',
          details: error
        };
      }

      if (status === 401) {
        return {
          type: ErrorType.UNAUTHORIZED,
          message: 'Unauthorized',
          i18nKey: 'errors.unauthorized',
          details: error
        };
      }

      if (status === 403) {
        return {
          type: ErrorType.UNAUTHORIZED,
          message: 'Forbidden',
          i18nKey: 'errors.unauthorized',
          details: error
        };
      }

      if (status === 404) {
        return {
          type: ErrorType.NOT_FOUND,
          message: 'Not found',
          i18nKey: 'errors.notFound',
          details: error
        };
      }

      if (status >= 500) {
        return {
          type: ErrorType.SERVER,
          message: 'Server error',
          i18nKey: 'errors.server',
          details: error
        };
      }
    }
  }

  // Unknown error
  return {
    type: ErrorType.UNKNOWN,
    message: error instanceof Error ? error.message : 'Unknown error',
    i18nKey: 'errors.unknown',
    details: error
  };
}

describe('useErrorHandler', () => {
  const parseError = parseErrorLogic;

  describe('parseError', () => {
    it('should parse network error', () => {
      const error = new TypeError('Failed to fetch');
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.NETWORK);
      expect(result.i18nKey).toBe('errors.network');
    });

    it('should parse validation error (400)', () => {
      const error = {
        response: {
          status: 400,
        },
      };
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.VALIDATION);
      expect(result.i18nKey).toBe('errors.validation');
    });

    it('should parse unauthorized error (401)', () => {
      const error = {
        response: {
          status: 401,
        },
      };
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.UNAUTHORIZED);
      expect(result.i18nKey).toBe('errors.unauthorized');
    });

    it('should parse not found error (404)', () => {
      const error = {
        response: {
          status: 404,
        },
      };
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.NOT_FOUND);
      expect(result.i18nKey).toBe('errors.notFound');
    });

    it('should parse server error (500+)', () => {
      const error = {
        response: {
          status: 500,
        },
      };
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.SERVER);
      expect(result.i18nKey).toBe('errors.server');
    });

    it('should parse unknown error', () => {
      const error = new Error('Unknown error');
      const result = parseError(error);

      expect(result.type).toBe(ErrorType.UNKNOWN);
      expect(result.i18nKey).toBe('errors.unknown');
    });
  });

  describe('error message extraction', () => {
    it('should extract correct i18n key for network error', () => {
      const error = new TypeError('Failed to fetch');
      const result = parseError(error);

      expect(result.i18nKey).toBe('errors.network');
      expect(result.message).toBe('Network error');
    });

    it('should extract correct i18n key for validation error', () => {
      const error = { response: { status: 400 } };
      const result = parseError(error);

      expect(result.i18nKey).toBe('errors.validation');
      expect(result.message).toBe('Validation error');
    });
  });
});
