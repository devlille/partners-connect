/**
 * Types d'erreurs API possibles
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
 * Interface pour les erreurs structurées
 */
export interface AppError {
  type: ErrorType;
  message: string;
  i18nKey: string;
  details?: unknown;
}

/**
 * Composable pour gérer les erreurs de manière centralisée
 */
export const useErrorHandler = () => {
  const { t } = useI18n();

  /**
   * Mappe une erreur API vers une erreur structurée avec clé i18n
   */
  const parseError = (error: unknown): AppError => {
    // Erreur réseau (pas de connexion)
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      return {
        type: ErrorType.NETWORK,
        message: 'Network error',
        i18nKey: 'errors.network',
        details: error
      };
    }

    // Erreur HTTP
    if (typeof error === 'object' && error !== null && 'status' in error) {
      const httpError = error as { status: number; data?: unknown };

      switch (httpError.status) {
        case 400:
          return {
            type: ErrorType.VALIDATION,
            message: 'Validation error',
            i18nKey: 'errors.validation',
            details: httpError.data
          };
        case 401:
        case 403:
          return {
            type: ErrorType.UNAUTHORIZED,
            message: 'Unauthorized',
            i18nKey: 'errors.unauthorized',
            details: httpError.data
          };
        case 404:
          return {
            type: ErrorType.NOT_FOUND,
            message: 'Not found',
            i18nKey: 'errors.notFound',
            details: httpError.data
          };
        case 500:
        case 502:
        case 503:
          return {
            type: ErrorType.SERVER,
            message: 'Server error',
            i18nKey: 'errors.server',
            details: httpError.data
          };
      }
    }

    // Erreur inconnue
    return {
      type: ErrorType.UNKNOWN,
      message: 'Unknown error',
      i18nKey: 'errors.unknown',
      details: error
    };
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
