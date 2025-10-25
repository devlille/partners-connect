/**
 * Types pour les modals
 */

import type { Variant } from '~/constants/ui';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | 'full';

export interface ModalState {
  isOpen: boolean;
  title?: string;
  size?: ModalSize;
}

export interface ConfirmModalProps {
  /** État d'ouverture du modal */
  isOpen: boolean;
  /** Titre du modal */
  title: string;
  /** Message de confirmation */
  message: string;
  /** Texte du bouton de confirmation */
  confirmText?: string;
  /** Texte du bouton d'annulation */
  cancelText?: string;
  /** Variante (définit la couleur) */
  variant?: Variant;
  /** État de chargement */
  loading?: boolean;
  /** Icône à afficher */
  icon?: string;
  /** Désactiver le bouton de confirmation */
  confirmDisabled?: boolean;
}

export interface FormModalProps {
  /** État d'ouverture du modal */
  isOpen: boolean;
  /** Titre du modal */
  title: string;
  /** Taille du modal */
  size?: ModalSize;
  /** État de chargement */
  loading?: boolean;
  /** Désactiver les actions pendant le chargement */
  disableWhileLoading?: boolean;
  /** Fermer en cliquant sur le backdrop */
  closeOnBackdropClick?: boolean;
  /** Fermer avec la touche ESC */
  closeOnEsc?: boolean;
}

export interface ModalEmits {
  /** Émis quand le modal est confirmé */
  (e: 'confirm'): void;
  /** Émis quand le modal est annulé */
  (e: 'cancel'): void;
  /** Émis quand le modal est fermé */
  (e: 'close'): void;
  /** Émis quand l'état d'ouverture change */
  (e: 'update:isOpen', value: boolean): void;
}

export interface UseModalOptions {
  /** Taille par défaut du modal */
  defaultSize?: ModalSize;
  /** Auto focus sur le premier élément */
  autoFocus?: boolean;
  /** Fermer avec ESC */
  closeOnEsc?: boolean;
  /** Fermer en cliquant sur le backdrop */
  closeOnBackdropClick?: boolean;
  /** Callback appelé à l'ouverture */
  onOpen?: () => void;
  /** Callback appelé à la fermeture */
  onClose?: () => void;
}

export interface UseModalReturn {
  /** État d'ouverture */
  isOpen: Ref<boolean>;
  /** Ouvrir le modal */
  open: () => void;
  /** Fermer le modal */
  close: () => void;
  /** Toggle l'état du modal */
  toggle: () => void;
  /** Taille du modal */
  size: Ref<ModalSize>;
  /** Changer la taille */
  setSize: (size: ModalSize) => void;
}
