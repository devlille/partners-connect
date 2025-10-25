/**
 * Discriminated union types for application errors
 * Provides automatic type narrowing based on the error type
 */

/**
 * Network error - failed to connect to server
 */
export interface NetworkError {
  type: 'network';
  message: string;
  i18nKey: 'errors.network';
  details: TypeError;
}

/**
 * Validation error - invalid data (400)
 */
export interface ValidationError {
  type: 'validation';
  message: string;
  i18nKey: 'errors.validation';
  details?: unknown;
}

/**
 * Unauthorized error - authentication/authorization failed (401, 403)
 */
export interface UnauthorizedError {
  type: 'unauthorized';
  message: string;
  i18nKey: 'errors.unauthorized';
  details?: unknown;
}

/**
 * Not found error - resource not found (404)
 */
export interface NotFoundError {
  type: 'not_found';
  message: string;
  i18nKey: 'errors.notFound';
  details?: unknown;
}

/**
 * Server error - internal server error (500, 502, 503)
 */
export interface ServerError {
  type: 'server';
  message: string;
  i18nKey: 'errors.server';
  details?: unknown;
}

/**
 * Unknown error - unhandled error type
 */
export interface UnknownError {
  type: 'unknown';
  message: string;
  i18nKey: 'errors.unknown';
  details?: unknown;
}

/**
 * Discriminated union of all application errors
 * TypeScript automatically narrows the type based on the 'type' property
 *
 * @example
 * ```typescript
 * const error = parseError(unknownError);
 * if (error.type === 'network') {
 *   // TypeScript knows this is NetworkError
 *   console.log(error.details); // TypeError
 * } else if (error.type === 'validation') {
 *   // TypeScript knows this is ValidationError
 *   // Can safely access validation-specific properties
 * }
 * ```
 */
export type AppError =
  | NetworkError
  | ValidationError
  | UnauthorizedError
  | NotFoundError
  | ServerError
  | UnknownError;

/**
 * Legacy enum for backward compatibility
 * @deprecated Use discriminated union types instead
 */
export enum ErrorType {
  NETWORK = 'network',
  VALIDATION = 'validation',
  UNAUTHORIZED = 'unauthorized',
  NOT_FOUND = 'not_found',
  SERVER = 'server',
  UNKNOWN = 'unknown'
}

/**
 * Composable pour gérer les erreurs de manière centralisée
 */
export const useErrorHandler = () => {
  const { t } = useI18n();

  /**
   * Mappe une erreur API vers une erreur structurée avec clé i18n
   * Returns a discriminated union type for automatic type narrowing
   */
  const parseError = (error: unknown): AppError => {
    // Erreur réseau (pas de connexion)
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      return {
        type: 'network',
        message: 'Network error',
        i18nKey: 'errors.network',
        details: error
      } satisfies NetworkError;
    }

    // Erreur HTTP
    if (typeof error === 'object' && error !== null && 'status' in error) {
      const httpError = error as { status: number; data?: unknown };

      switch (httpError.status) {
        case 400:
          return {
            type: 'validation',
            message: 'Validation error',
            i18nKey: 'errors.validation',
            details: httpError.data
          } satisfies ValidationError;
        case 401:
        case 403:
          return {
            type: 'unauthorized',
            message: 'Unauthorized',
            i18nKey: 'errors.unauthorized',
            details: httpError.data
          } satisfies UnauthorizedError;
        case 404:
          return {
            type: 'not_found',
            message: 'Not found',
            i18nKey: 'errors.notFound',
            details: httpError.data
          } satisfies NotFoundError;
        case 500:
        case 502:
        case 503:
          return {
            type: 'server',
            message: 'Server error',
            i18nKey: 'errors.server',
            details: httpError.data
          } satisfies ServerError;
      }
    }

    // Erreur inconnue
    return {
      type: 'unknown',
      message: 'Unknown error',
      i18nKey: 'errors.unknown',
      details: error
    } satisfies UnknownError;
  };

  /**
   * Récupère le message d'erreur traduit
   */
  const getErrorMessage = (error: unknown): string => {
    const appError = parseError(error);
    return t(appError.i18nKey);
  };

  /**
   * Log l'erreur en console avec contexte
   */
  const logError = (error: unknown, context?: string) => {
    const appError = parseError(error);
    const prefix = context ? `[${context}]` : '';
    console.error(`${prefix} ${appError.type}:`, appError.message, appError.details);
  };

  /**
   * Gère une erreur complète : log + récupération du message
   */
  const handleError = (error: unknown, context?: string): string => {
    logError(error, context);
    return getErrorMessage(error);
  };

  return {
    parseError,
    getErrorMessage,
    logError,
    handleError,
    ErrorType
  };
};
