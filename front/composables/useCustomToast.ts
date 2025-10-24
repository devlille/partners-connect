/**
 * Type de notification
 */
export enum ToastType {
  SUCCESS = 'success',
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info'
}

/**
 * Interface pour une notification
 */
export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

/**
 * État global des notifications
 */
const toasts = ref<Toast[]>([]);

/**
 * Composable pour gérer les notifications toast personnalisées
 */
export const useCustomToast = () => {
  /**
   * Ajouter une notification
   */
  const addToast = (type: ToastType, message: string, duration: number = 5000) => {
    const id = `toast-${Date.now()}-${Math.random()}`;
    const toast: Toast = { id, type, message, duration };

    toasts.value.push(toast);

    // Supprimer automatiquement après la durée spécifiée
    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }

    return id;
  };

  /**
   * Supprimer une notification
   */
  const removeToast = (id: string) => {
    toasts.value = toasts.value.filter(t => t.id !== id);
  };

  /**
   * Afficher une notification de succès
   */
  const success = (message: string, duration?: number) => {
    return addToast(ToastType.SUCCESS, message, duration);
  };

  /**
   * Afficher une notification d'erreur
   */
  const error = (message: string, duration?: number) => {
    return addToast(ToastType.ERROR, message, duration);
  };

  /**
   * Afficher une notification d'avertissement
   */
  const warning = (message: string, duration?: number) => {
    return addToast(ToastType.WARNING, message, duration);
  };

  /**
   * Afficher une notification d'information
   */
  const info = (message: string, duration?: number) => {
    return addToast(ToastType.INFO, message, duration);
  };

  /**
   * Effacer toutes les notifications
   */
  const clearAll = () => {
    toasts.value = [];
  };

  return {
    toasts: readonly(toasts),
    addToast,
    removeToast,
    success,
    error,
    warning,
    info,
    clearAll,
    ToastType
  };
};
