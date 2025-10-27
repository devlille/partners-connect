import type { ExtendedPartnershipItem } from '~/types/partnership';
import { PARTNERSHIP_ACTION_ERRORS, PARTNERSHIP_ACTION_SUCCESS } from '~/constants/partnership';

/**
 * Formater les messages d'erreur API pour les actions de partenariat
 * @param err - L'erreur retournée par l'API
 * @param action - Le type d'action ('validate' | 'decline')
 * @returns Message d'erreur formaté
 */
export function formatPartnershipActionError(err: any, action: 'validate' | 'decline'): string {
  if (err.response) {
    if (err.response.status === 404) {
      return PARTNERSHIP_ACTION_ERRORS.NOT_FOUND;
    }
    if (err.response.status === 403) {
      return PARTNERSHIP_ACTION_ERRORS.FORBIDDEN;
    }
  }

  return action === 'validate'
    ? PARTNERSHIP_ACTION_ERRORS.VALIDATION_GENERIC
    : PARTNERSHIP_ACTION_ERRORS.DECLINE_GENERIC;
}

/**
 * Composable pour gérer les actions sur les partenariats
 */
export const usePartnershipActions = () => {
  const toast = useCustomToast();

  /**
   * Valider un partenariat
   * @param partnership - Le partenariat à valider
   * @param orgSlug - Slug de l'organisation
   * @param eventSlug - Slug de l'événement
   * @param sponsorId - ID du sponsor
   * @param onSuccess - Callback à exécuter en cas de succès
   */
  async function validatePartnership(
    partnership: ExtendedPartnershipItem,
    orgSlug: string,
    eventSlug: string,
    sponsorId: string,
    onSuccess?: () => Promise<void>
  ): Promise<{ success: boolean; error?: string }> {
    try {
      const { postOrgsEventsPartnershipValidate } = await import('~/utils/api');

      await postOrgsEventsPartnershipValidate(orgSlug, eventSlug, sponsorId);

      toast.success(PARTNERSHIP_ACTION_SUCCESS.VALIDATED(partnership.company_name));

      if (onSuccess) {
        await onSuccess();
      }

      return { success: true };
    } catch (err: any) {
      console.error('Failed to validate partnership:', err);
      const errorMessage = formatPartnershipActionError(err, 'validate');
      toast.error(errorMessage);
      return { success: false, error: errorMessage };
    }
  }

  /**
   * Refuser un partenariat
   * @param partnership - Le partenariat à refuser
   * @param orgSlug - Slug de l'organisation
   * @param eventSlug - Slug de l'événement
   * @param sponsorId - ID du sponsor
   * @param onSuccess - Callback à exécuter en cas de succès
   */
  async function declinePartnership(
    partnership: ExtendedPartnershipItem,
    orgSlug: string,
    eventSlug: string,
    sponsorId: string,
    onSuccess?: () => void
  ): Promise<{ success: boolean; error?: string }> {
    try {
      const { postOrgsEventsPartnershipDecline } = await import('~/utils/api');

      await postOrgsEventsPartnershipDecline(orgSlug, eventSlug, sponsorId);

      toast.success(PARTNERSHIP_ACTION_SUCCESS.DECLINED(partnership.company_name));

      if (onSuccess) {
        onSuccess();
      }

      return { success: true };
    } catch (err: any) {
      console.error('Failed to decline partnership:', err);
      const errorMessage = formatPartnershipActionError(err, 'decline');
      toast.error(errorMessage);
      return { success: false, error: errorMessage };
    }
  }

  return {
    validatePartnership,
    declinePartnership,
    formatPartnershipActionError
  };
};
