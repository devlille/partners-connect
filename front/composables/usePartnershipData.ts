/**
 * Composable for fetching partnership data
 *
 * Provides shared data fetching logic for both authenticated and public contexts.
 * Handles loading states, error handling, and data mapping.
 *
 * Feature: 001-public-partnership-view
 * Created: 2025-11-18
 */

import type { ExtendedPartnershipItem } from "~/types/partnership";
import type { Ref } from "vue";

/**
 * Company data structure (TBD from backend)
 */
export interface CompanyData {
  id: string;
  name: string;
  address?: string | null;
  city?: string | null;
  postal_code?: string | null;
  country?: string | null;
  website?: string | null;
  logo_url?: string | null;
}

/**
 * Composable for fetching and managing partnership data
 *
 * @param partnershipId - Partnership UUID
 * @param isPublic - Whether to use public (unauthenticated) or authenticated endpoints
 * @returns Object with reactive state and data fetching functions
 */
export const usePartnershipData = (
  partnershipId: string | Ref<string>,
  isPublic: boolean = false,
) => {
  // Convert to ref if it's not already
  const id = isRef(partnershipId) ? partnershipId : ref(partnershipId);

  // Reactive state
  const partnership = ref<ExtendedPartnershipItem | null>(null);
  const company = ref<CompanyData | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /**
   * Map backend response to frontend ExtendedPartnershipItem format
   */
  const mapPartnershipData = (data: any): ExtendedPartnershipItem => {
    // TODO: This mapping depends on the actual backend response structure
    // For now, assuming the response matches ExtendedPartnershipItem format
    return {
      id: data.id,
      contact: {
        display_name: data.contact_name || data.contact?.display_name || "",
        role: data.contact_role || data.contact?.role || "",
      },
      company_name: data.company_name || data.company?.name || "",
      event_name: data.event_name || data.event?.name || "",
      selected_pack_id: data.selected_pack?.id || data.selected_pack_id || null,
      selected_pack_name: data.selected_pack?.name || data.selected_pack_name || null,
      suggested_pack_id: data.suggested_pack?.id || data.suggested_pack_id || null,
      suggested_pack_name: data.suggested_pack?.name || data.suggested_pack_name || null,
      validated_pack_id: data.validated_pack?.id || data.validated_pack_id || null,
      language: data.language || "fr",
      phone: data.phone || null,
      emails: Array.isArray(data.emails) ? data.emails.join(", ") : data.emails || "",
      created_at: data.created_at || "",
      validated: data.validated || data.process_status?.validated_at !== null || false,
      paid: data.paid || data.process_status?.billing_status === "paid" || false,
      suggestion: data.suggestion || data.suggested_pack !== null || false,
      agreement_generated:
        data.agreement_generated || data.process_status?.agreement_url !== null || false,
      agreement_signed:
        data.agreement_signed || data.process_status?.agreement_signed_url !== null || false,
      option_ids: data.option_ids || data.selected_pack?.options?.map((o: any) => o.id) || [],
      pack_options: data.pack_options || data.selected_pack?.options || [],
    };
  };

  /**
   * Load partnership data from the appropriate endpoint
   */
  const loadPartnership = async (eventSlug?: string) => {
    try {
      loading.value = true;
      error.value = null;

      if (isPublic) {
        // Use public endpoint (no authentication required)
        // Public endpoints are in utils/api.ts and start with /events/ instead of /orgs/
        const { getEventsPartnershipDetailed } = await import("~/utils/api");

        // For public access, we need the eventSlug
        // This will be provided by the public page route
        if (!eventSlug) {
          throw new Error("eventSlug is required for public partnership access");
        }

        const response = await getEventsPartnershipDetailed(eventSlug, id.value);

        if (response.ok && response.data) {
          partnership.value = mapPartnershipData(response.data.partnership);
          company.value = response.data.company || null;
        } else {
          throw new Error("Failed to load partnership data");
        }
      } else {
        // Use authenticated endpoint (starts with /orgs/)
        // This requires both orgSlug and eventSlug which we don't have in this context
        // For now, this will be handled by the authenticated page directly
        throw new Error("Authenticated data fetching not yet implemented in composable");
      }
    } catch (err: any) {
      console.error("Failed to load partnership:", err);

      // Handle specific error cases
      if (err.response) {
        if (err.response.status === 404) {
          error.value = isPublic ? "Partenariat non trouvé" : "Partenariat non trouvé";
        } else if (err.response.status === 500) {
          error.value = "Erreur serveur. Veuillez rafraîchir la page.";
        } else {
          error.value = "Impossible de charger les informations du partenariat";
        }
      } else {
        error.value = "Erreur réseau. Veuillez vérifier votre connexion.";
      }
    } finally {
      loading.value = false;
    }
  };

  /**
   * Refresh partnership data
   */
  const refresh = (eventSlug?: string) => loadPartnership(eventSlug);

  // Note: Auto-load is NOT done here because we need the eventSlug
  // The public page will call loadPartnership(eventSlug) explicitly after getting the slug

  return {
    // Reactive state
    partnership: readonly(partnership),
    company: readonly(company),
    loading: readonly(loading),
    error: readonly(error),

    // Actions
    loadPartnership,
    refresh,
  };
};
