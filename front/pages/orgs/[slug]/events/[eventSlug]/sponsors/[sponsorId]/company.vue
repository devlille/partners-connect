<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Entreprise</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'entreprise</h2>

        <TableSkeleton v-if="loadingCompany" :columns="4" :rows="6" />

        <div v-else-if="companyError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {{ companyError }}
        </div>

        <div v-else-if="company">
         

          <PartnershipCompanyForm
            :company="company"
            :loading="savingCompany"
            @save="handleSaveCompany"
          />

          <div v-if="companyFormError" class="text-sm text-red-600 mt-4">
            {{ companyFormError }}
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, putCompanyById, type CompanySchema, type MediaSchema, type UpdateCompanySchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

const { footerLinks } = useDashboardLinks();
const { orgSlug, eventSlug, sponsorId } = useRouteParams();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const company = ref<CompanySchema | null>(null);
const loadingCompany = ref(false);
const companyError = ref<string | null>(null);

const savingCompany = ref(false);
const companyFormError = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    loadingCompany.value = true;
    error.value = null;
    companyError.value = null;

    // Un seul appel API pour charger le partenariat et l'entreprise
    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    const { partnership: p, company: c, event } = response.data;

    // Stocker l'entreprise
    company.value = c;

    // Extraire les options du pack
    const packOptions = ((p.selected_pack as any)?.options || p.selected_pack?.optional_options || [])
      .map((opt: any) => ({
        id: opt.id,
        name: opt.name,
        description: opt.description || null
      }));

    // Mapper les données du partenariat
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
      option_ids: packOptions.map(opt => opt.id),
      pack_options: packOptions
    };
  } catch (err: any) {
    console.error('Failed to load partnership:', err);

    if (err.response?.status === 404) {
      error.value = 'Sponsor non trouvé';
      companyError.value = 'Partenariat ou entreprise introuvable';
    } else {
      error.value = 'Impossible de charger les informations du sponsor';
      companyError.value = 'Impossible de charger les informations de l\'entreprise';
    }
  } finally {
    loading.value = false;
    loadingCompany.value = false;
  }
}

async function handleSaveCompany(updateData: UpdateCompanySchema) {
  if (!company.value) {
    console.warn('Cannot save: company data not loaded');
    return;
  }

  companyFormError.value = null;

  try {
    savingCompany.value = true;

    await putCompanyById(company.value.id, updateData);

    // Recharger uniquement les données de l'entreprise
    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    company.value = response.data.company;

    // Afficher un message de succès
    const toast = useCustomToast();
    toast.success('Les informations de l\'entreprise ont été mises à jour avec succès');
  } catch (err: any) {
    console.error('Failed to save company:', err);

    // Messages d'erreur par code HTTP
    const errorMessages: Record<number, string> = {
      404: 'Entreprise introuvable',
      403: 'Vous n\'êtes pas autorisé à modifier cette entreprise',
      400: 'Données invalides'
    };

    const errorMessage = err.response?.data?.message
      || errorMessages[err.response?.status]
      || 'Impossible de sauvegarder les modifications';

    companyFormError.value = errorMessage;

    // Afficher aussi l'erreur dans un toast
    const toast = useCustomToast();
    toast.error(errorMessage);
  } finally {
    savingCompany.value = false;
  }
}

function handleLogoUploaded(media: MediaSchema) {
  if (company.value) {
    company.value = {
      ...company.value,
      medias: media
    };
  }
}

function handleLogoError(errorMessage: string) {
  companyError.value = errorMessage;
}

onMounted(() => {
  loadPartnership();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `Entreprise - ${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
