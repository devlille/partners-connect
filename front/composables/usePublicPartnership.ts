import { getEventsPartnershipDetailed, getEventsPartnershipBilling, putCompanyById, putEventsPartnershipBilling } from "~/utils/api";
import type { ExtendedPartnershipItem } from "~/types/partnership";
import type { CompanyBillingData, UpdateCompanySchema } from "~/utils/api";

export const usePublicPartnership = () => {
  const toast = useToast();
  const { eventSlug, partnershipId } = useRouteParams();

  const partnership = useState<ExtendedPartnershipItem | null>('partnership', () => null);
  const company = useState<any | null>('company', () => null);
  const billing = useState<CompanyBillingData | null>('billing', () => null);
  const loading = useState<boolean>('partnership-loading', () => true);
  const error = useState<string | null>('partnership-error', () => null);
  const savingCompany = useState<boolean>('saving-company', () => false);
  const savingBilling = useState<boolean>('saving-billing', () => false);

  /**
   * Load partnership data using public API endpoints
   */
  async function loadPartnership() {
    try {
      loading.value = true;
      error.value = null;

      // Load partnership details
      const response = await getEventsPartnershipDetailed(eventSlug.value, partnershipId.value);
      const { partnership: p, company: c, event } = response.data;

      // Store company data
      company.value = c;

      // Load billing data
      try {
        const billingResponse = await getEventsPartnershipBilling(eventSlug.value, partnershipId.value);
        billing.value = billingResponse.data || null;
      } catch (billingErr: any) {
        // If billing doesn't exist yet, it's not an error
        if (billingErr.response?.status !== 404) {
          console.error('Failed to load billing data:', billingErr);
        }
        billing.value = null;
      }

      // Extract pack options
      const packOptions = ((p.selected_pack as any)?.options || p.selected_pack?.optional_options || []).map((opt: any) => ({
        id: opt.id,
        name: opt.name,
        description: opt.description || null
      }));
      const optionIds = packOptions.map(opt => opt.id);

      // Map data to ExtendedPartnershipItem
      partnership.value = {
        id: p.id,
        contact: {
          display_name: p.contact_name,
          role: p.contact_role
        },
        company_name: c.name,
        event_name: event.name,
        selected_pack_id: p.selected_pack?.id || null,
        selected_pack_name: p.selected_pack?.name || null,
        suggested_pack_id: p.suggestion_pack?.id || null,
        suggested_pack_name: p.suggestion_pack?.name || null,
        validated_pack_id: p.validated_pack?.id || null,
        language: p.language,
        phone: p.phone || null,
        emails: p.emails.join(', '),
        created_at: p.created_at,
        validated: p.process_status?.validated_at !== null && p.process_status?.validated_at !== undefined,
        paid: p.process_status?.billing_status?.toLowerCase() === 'paid',
        suggestion: false,
        agreement_generated: p.process_status?.agreement_url !== null && p.process_status?.agreement_url !== undefined,
        agreement_signed: p.process_status?.agreement_signed_url !== null && p.process_status?.agreement_signed_url !== undefined,
        agreement_url: p.process_status?.agreement_url || null,
        agreement_signed_url: p.process_status?.agreement_signed_url || null,
        quote_url: p.process_status?.quote_url || null,
        invoice_url: p.process_status?.invoice_url || null,
        option_ids: optionIds,
        pack_options: packOptions
      };
    } catch (err: any) {
      console.error('Failed to load partnership:', err);

      if (err.response?.status === 404) {
        error.value = `404 Not Found: Partnership not found for event "${eventSlug.value}" and ID "${partnershipId.value}". Please verify the URL.`;
      } else if (err.response?.status >= 500) {
        const statusText = err.response?.statusText || 'Internal Server Error';
        error.value = `${err.response.status} Server Error: ${statusText}. The server encountered an error processing your request. Please try again later or contact support if the problem persists.`;
      } else if (!navigator.onLine) {
        error.value = 'Network Error: No internet connection detected. Please check your network connection and refresh the page to retry.';
      } else if (err.response?.status) {
        error.value = `HTTP ${err.response.status} Error: ${err.response.statusText || err.message || 'Failed to load partnership data'}. Please try again or contact support.`;
      } else {
        error.value = `Error: ${err.message || 'Unknown error occurred while loading partnership data'}. Please refresh the page to retry.`;
      }
    } finally {
      loading.value = false;
    }
  }

  /**
   * Handle company information save
   */
  async function handleCompanySave(data: UpdateCompanySchema) {
    if (!company.value?.id) return;

    try {
      savingCompany.value = true;
      error.value = null;

      await putCompanyById(company.value.id, data);

      // Reload partnership data to get updated information
      await loadPartnership();

      // Show success message to user
      toast.add({
        title: 'Informations mises à jour',
        description: 'Les informations de l\'entreprise ont été enregistrées avec succès',
        color: 'success',
        timeout: 3000
      });
    } catch (err: any) {
      console.error('Failed to update company:', err);
      const errorMessage = `Impossible de mettre à jour les informations de l'entreprise: ${err.message || 'Erreur inconnue'}`;
      error.value = errorMessage;

      toast.add({
        title: 'Erreur de sauvegarde',
        description: errorMessage,
        color: 'error',
        timeout: 5000
      });
    } finally {
      savingCompany.value = false;
    }
  }

  /**
   * Handle billing information save
   */
  async function handleBillingSave(data: CompanyBillingData) {
    try {
      savingBilling.value = true;
      error.value = null;

      await putEventsPartnershipBilling(eventSlug.value, partnershipId.value, data);

      // Reload partnership data to get updated billing
      await loadPartnership();

      // Show success message to user
      toast.add({
        title: 'Informations mises à jour',
        description: 'Les informations de facturation ont été enregistrées avec succès',
        color: 'success',
        timeout: 3000
      });
    } catch (err: any) {
      console.error('Failed to update billing:', err);
      const errorMessage = `Impossible de mettre à jour les informations de facturation: ${err.message || 'Erreur inconnue'}`;
      error.value = errorMessage;

      toast.add({
        title: 'Erreur de sauvegarde',
        description: errorMessage,
        color: 'error',
        timeout: 5000
      });
    } finally {
      savingBilling.value = false;
    }
  }

  return {
    eventSlug,
    partnershipId,
    partnership,
    company,
    billing,
    loading,
    error,
    savingCompany,
    savingBilling,
    loadPartnership,
    handleCompanySave,
    handleBillingSave
  };
};
