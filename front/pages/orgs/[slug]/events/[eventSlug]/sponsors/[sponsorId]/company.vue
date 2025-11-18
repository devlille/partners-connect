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
          <div class="border-b pb-4 mb-4">
            <LogoUpload
              :company-id="company.id"
              :company-name="company.name"
              :current-logo-media="company.medias"
              :disabled="savingCompany"
              @uploaded="handleLogoUploaded"
              @error="handleLogoError"
            />
          </div>

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
import { getOrgsEventsPartnership, getCompanies, getCompaniesPartnership, putCompanyById, type CompanySchema, type MediaSchema, type UpdateCompanySchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
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
    error.value = null;

    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const found = response.data.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;

    await loadCompanyInfo();
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

async function loadCompanyInfo() {
  try {
    loadingCompany.value = true;
    companyError.value = null;

    const companiesResponse = await getCompanies();
    const companies = companiesResponse.data.items;

    const partnershipChecks = await Promise.all(
      companies.map(async (comp) => {
        try {
          const partnershipsResponse = await getCompaniesPartnership(comp.id);
          const hasPartnership = partnershipsResponse.data.find((p: any) => p.id === sponsorId.value);
          return hasPartnership ? comp : null;
        } catch (err) {
          console.warn(`Failed to check partnerships for company ${comp.id}:`, err);
          return null;
        }
      })
    );

    const foundCompany = partnershipChecks.find(comp => comp !== null);

    if (foundCompany) {
      company.value = foundCompany;
    } else {
      companyError.value = 'Entreprise non trouvée';
    }
  } catch (err) {
    console.error('Failed to load company info:', err);
    companyError.value = 'Impossible de charger les informations de l\'entreprise';
  } finally {
    loadingCompany.value = false;
  }
}

async function handleSaveCompany(updateData: UpdateCompanySchema) {
  if (!company.value) return;

  companyFormError.value = null;

  try {
    savingCompany.value = true;

    await putCompanyById(company.value.id, updateData);

    // Recharger les données de l'entreprise
    await loadCompanyInfo();

    // Afficher un message de succès
    const toast = useCustomToast();
    toast.success('Les informations de l\'entreprise ont été mises à jour avec succès');
  } catch (err: any) {
    console.error('Failed to save company:', err);

    // Essayer d'extraire le message d'erreur du serveur
    let errorMessage: string;
    if (err.response?.data?.message) {
      errorMessage = err.response.data.message;
    } else if (err.response) {
      if (err.response.status === 404) {
        errorMessage = 'Entreprise introuvable';
      } else if (err.response.status === 403) {
        errorMessage = 'Vous n\'êtes pas autorisé à modifier cette entreprise';
      } else {
        errorMessage = 'Impossible de sauvegarder les modifications';
      }
    } else {
      errorMessage = 'Impossible de sauvegarder les modifications';
    }

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
