/**
 * Composable pour gérer l'état et le comportement des modals
 * Inclut gestion du focus trap, fermeture ESC, et accessibilité
 */

import type { UseModalOptions, UseModalReturn, ModalSize } from '~/types/modal';

export function useModal(options: UseModalOptions = {}): UseModalReturn {
  const {
    defaultSize = 'md',
    autoFocus = true,
    closeOnEsc = true,
    closeOnBackdropClick = true,
    onOpen,
    onClose,
  } = options;

  const isOpen = ref(false);
  const size = ref<ModalSize>(defaultSize);

  /**
   * Ouvrir le modal
   */
  function open() {
    isOpen.value = true;
    onOpen?.();

    // Auto focus sur le premier élément focusable
    if (autoFocus && import.meta.client) {
      nextTick(() => {
        const modal = document.querySelector('[role="dialog"]');
        if (modal) {
          const focusable = modal.querySelector<HTMLElement>(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
          );
          focusable?.focus();
        }
      });
    }

    // Empêcher le scroll du body
    if (import.meta.client) {
      document.body.style.overflow = 'hidden';
    }
  }

  /**
   * Fermer le modal
   */
  function close() {
    isOpen.value = false;
    onClose?.();

    // Restaurer le scroll du body
    if (import.meta.client) {
      document.body.style.overflow = '';
    }
  }

  /**
   * Toggle le modal
   */
  function toggle() {
    if (isOpen.value) {
      close();
    } else {
      open();
    }
  }

  /**
   * Changer la taille du modal
   */
  function setSize(newSize: ModalSize) {
    size.value = newSize;
  }

  // Fermer avec la touche ESC
  if (closeOnEsc && import.meta.client) {
    const handleEscapeKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen.value) {
        close();
      }
    };

    // Ajouter l'event listener immédiatement
    document.addEventListener('keydown', handleEscapeKey);

    // Cleanup au unmount
    onUnmounted(() => {
      document.removeEventListener('keydown', handleEscapeKey);
      if (isOpen.value) {
        document.body.style.overflow = '';
      }
    });
  } else {
    // Cleanup même sans ESC listener
    onUnmounted(() => {
      if (import.meta.client && isOpen.value) {
        document.body.style.overflow = '';
      }
    });
  }

  return {
    isOpen,
    open,
    close,
    toggle,
    size,
    setSize,
  };
}
