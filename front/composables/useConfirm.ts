/**
 * Options pour la modale de confirmation
 */
export interface ConfirmOptions {
  /** Titre de la modale */
  title: string;
  /** Message de confirmation */
  message: string;
  /** Label du bouton de confirmation */
  confirmLabel?: string;
  /** Label du bouton d'annulation */
  cancelLabel?: string;
  /** Type de confirmation (danger, warning, info) */
  type?: 'danger' | 'warning' | 'info';
}

/**
 * État de la modale de confirmation
 */
interface ConfirmState {
  isOpen: boolean;
  options: ConfirmOptions;
  confirming: boolean;
  resolve: ((value: boolean) => void) | null;
}

const state = reactive<ConfirmState>({
  isOpen: false,
  options: {
    title: '',
    message: '',
    confirmLabel: 'Confirmer',
    cancelLabel: 'Annuler',
    type: 'info'
  },
  confirming: false,
  resolve: null
});

/**
 * Composable pour afficher une modale de confirmation
 *
 * @example
 * const { confirm } = useConfirm();
 *
 * const confirmed = await confirm({
 *   title: 'Confirmer l\'action',
 *   message: 'Êtes-vous sûr de vouloir continuer ?',
 *   type: 'danger'
 * });
 *
 * if (confirmed) {
 *   // Action confirmée
 * }
 */
export const useConfirm = () => {
  /**
   * Afficher la modale de confirmation et attendre la réponse
   * @param options - Options de la modale
   * @returns Promise qui se résout à true si confirmé, false si annulé
   */
  const confirm = (options: ConfirmOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      state.options = {
        confirmLabel: 'Confirmer',
        cancelLabel: 'Annuler',
        type: 'info',
        ...options
      };
      state.isOpen = true;
      state.confirming = false;
      state.resolve = resolve;
    });
  };

  /**
   * Gérer la confirmation
   */
  const handleConfirm = () => {
    if (state.resolve) {
      state.resolve(true);
      state.isOpen = false;
      state.resolve = null;
    }
  };

  /**
   * Gérer l'annulation
   */
  const handleCancel = () => {
    if (state.resolve) {
      state.resolve(false);
      state.isOpen = false;
      state.resolve = null;
    }
  };

  /**
   * Définir l'état de chargement pendant la confirmation
   */
  const setConfirming = (value: boolean) => {
    state.confirming = value;
  };

  return {
    // État réactif
    confirmState: readonly(state),

    // Méthodes
    confirm,
    handleConfirm,
    handleCancel,
    setConfirming
  };
};
