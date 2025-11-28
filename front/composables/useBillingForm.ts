import type { CompanyBillingData } from '~/utils/api';
import { EMAIL_REGEX } from '~/constants/validation';

/**
 * Composable pour gérer la logique du formulaire de facturation
 * Partage la validation et la gestion du formulaire entre les composants
 */
export const useBillingForm = (initialBilling?: CompanyBillingData | null) => {

  const form = ref<CompanyBillingData>({
    name: initialBilling?.name || null,
    po: initialBilling?.po || null,
    contact: {
      first_name: initialBilling?.contact?.first_name || '',
      last_name: initialBilling?.contact?.last_name || '',
      email: initialBilling?.contact?.email || ''
    }
  });

  /**
   * Vérifie si le formulaire est valide
   * Tous les champs du contact sont requis et l'email doit être valide
   */
  const isFormValid = computed(() => {
    return (
      form.value.contact.first_name.trim() !== '' &&
      form.value.contact.last_name.trim() !== '' &&
      form.value.contact.email.trim() !== '' &&
      EMAIL_REGEX.test(form.value.contact.email)
    );
  });

  /**
   * Réinitialise le formulaire avec de nouvelles données
   */
  const resetForm = (billing: CompanyBillingData | null) => {
    if (billing) {
      form.value = {
        name: billing.name || null,
        po: billing.po || null,
        contact: {
          first_name: billing.contact?.first_name || '',
          last_name: billing.contact?.last_name || '',
          email: billing.contact?.email || ''
        }
      };
    }
  };

  /**
   * Prépare les données du formulaire pour l'envoi
   * Trim les espaces et normalise les données
   */
  const prepareBillingData = (): CompanyBillingData => {
    return {
      name: form.value.name || null,
      po: form.value.po || null,
      contact: {
        first_name: form.value.contact.first_name.trim(),
        last_name: form.value.contact.last_name.trim(),
        email: form.value.contact.email.trim()
      }
    };
  };

  return {
    form,
    isFormValid,
    resetForm,
    prepareBillingData
  };
};
