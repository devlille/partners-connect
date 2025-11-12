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

        <div v-else-if="company" class="space-y-4">
          <div>
            <label for="company-name" class="block text-sm font-medium text-gray-700 mb-2">
              Nom <span class="text-red-500">*</span>
            </label>
            <UInput
              id="company-name"
              v-model="companyForm.name"
              placeholder="Nom de l'entreprise"
              :disabled="savingCompany"
              class="w-full"
            />
          </div>

          <div>
            <label for="company-siret" class="block text-sm font-medium text-gray-700 mb-2">
              SIRET <span class="text-red-500">*</span>
            </label>
            <UInput
              id="company-siret"
              v-model="companyForm.siret"
              placeholder="12345678901234"
              :disabled="savingCompany"
              class="w-full"
            />
          </div>

          <div>
            <label for="company-vat" class="block text-sm font-medium text-gray-700 mb-2">
              TVA <span class="text-red-500">*</span>
            </label>
            <UInput
              id="company-vat"
              v-model="companyForm.vat"
              placeholder="FR12345678901"
              :disabled="savingCompany"
              class="w-full"
            />
          </div>

          <div>
            <label for="company-site-url" class="block text-sm font-medium text-gray-700 mb-2">
              Site web <span class="text-red-500">*</span>
            </label>
            <UInput
              id="company-site-url"
              v-model="companyForm.site_url"
              type="url"
              placeholder="https://example.com"
              :disabled="savingCompany"
              class="w-full"
            />
          </div>

          <div>
            <label for="company-description" class="block text-sm font-medium text-gray-700 mb-2">
              Description (optionnel)
            </label>
            <UTextarea
              id="company-description"
              v-model="companyForm.description"
              placeholder="Description de l'entreprise"
              :disabled="savingCompany"
              :rows="4"
              class="w-full"
            />
          </div>

          <div class="border-t pt-4">
            <LogoUpload
              v-if="company"
              :company-id="company.id"
              :company-name="company.name"
              :current-logo-media="company.medias"
              :disabled="savingCompany"
              @uploaded="handleLogoUploaded"
              @error="handleLogoError"
            />
          </div>

          <div class="border-t pt-4">
            <h3 class="text-sm font-medium text-gray-900 mb-3">Adresse du siège social</h3>

            <div class="space-y-4">
              <div>
                <label for="company-address" class="block text-sm font-medium text-gray-700 mb-2">
                  Adresse <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="company-address"
                  v-model="companyForm.head_office.address"
                  placeholder="123 Rue de la République"
                  :disabled="savingCompany"
                  class="w-full"
                />
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label for="company-zip" class="block text-sm font-medium text-gray-700 mb-2">
                    Code postal <span class="text-red-500">*</span>
                  </label>
                  <UInput
                    id="company-zip"
                    v-model="companyForm.head_office.zip_code"
                    placeholder="75001"
                    :disabled="savingCompany"
                    class="w-full"
                  />
                </div>

                <div>
                  <label for="company-city" class="block text-sm font-medium text-gray-700 mb-2">
                    Ville <span class="text-red-500">*</span>
                  </label>
                  <UInput
                    id="company-city"
                    v-model="companyForm.head_office.city"
                    placeholder="Paris"
                    :disabled="savingCompany"
                    class="w-full"
                  />
                </div>
              </div>

              <div>
                <label for="company-country" class="block text-sm font-medium text-gray-700 mb-2">
                  Pays <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="company-country"
                  v-model="companyForm.head_office.country"
                  placeholder="France"
                  :disabled="savingCompany"
                  class="w-full"
                />
              </div>
            </div>
          </div>

          <div v-if="companyFormError" class="text-sm text-red-600">
            {{ companyFormError }}
          </div>

          <div class="flex justify-end gap-3 pt-4">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="savingCompany"
              @click="resetCompanyForm"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="savingCompany"
              @click="handleSaveCompany"
            >
              Enregistrer
            </UButton>
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getCompanies, getCompaniesPartnership, type CompanySchema, type MediaSchema } from "~/utils/api";
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

const companyForm = ref({
  name: '',
  siret: '',
  vat: '',
  site_url: '',
  description: '',
  head_office: {
    address: '',
    city: '',
    zip_code: '',
    country: ''
  }
});

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

      companyForm.value = {
        name: foundCompany.name,
        siret: foundCompany.siret,
        vat: foundCompany.vat,
        site_url: foundCompany.site_url,
        description: foundCompany.description || '',
        head_office: {
          address: foundCompany.head_office.address,
          city: foundCompany.head_office.city,
          zip_code: foundCompany.head_office.zip_code,
          country: foundCompany.head_office.country
        }
      };
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

function resetCompanyForm() {
  if (!company.value) return;

  companyForm.value = {
    name: company.value.name,
    siret: company.value.siret,
    vat: company.value.vat,
    site_url: company.value.site_url,
    description: company.value.description || '',
    head_office: {
      address: company.value.head_office.address,
      city: company.value.head_office.city,
      zip_code: company.value.head_office.zip_code,
      country: company.value.head_office.country
    }
  };
  companyFormError.value = null;
}

async function handleSaveCompany() {
  companyFormError.value = null;

  // Validation
  if (!companyForm.value.name || !companyForm.value.name.trim()) {
    companyFormError.value = 'Le nom est obligatoire';
    return;
  }

  if (!companyForm.value.siret || !companyForm.value.siret.trim()) {
    companyFormError.value = 'Le SIRET est obligatoire';
    return;
  }

  if (!companyForm.value.vat || !companyForm.value.vat.trim()) {
    companyFormError.value = 'La TVA est obligatoire';
    return;
  }

  if (!companyForm.value.site_url || !companyForm.value.site_url.trim()) {
    companyFormError.value = 'Le site web est obligatoire';
    return;
  }

  if (!companyForm.value.head_office.address || !companyForm.value.head_office.address.trim()) {
    companyFormError.value = 'L\'adresse est obligatoire';
    return;
  }

  if (!companyForm.value.head_office.zip_code || !companyForm.value.head_office.zip_code.trim()) {
    companyFormError.value = 'Le code postal est obligatoire';
    return;
  }

  if (!companyForm.value.head_office.city || !companyForm.value.head_office.city.trim()) {
    companyFormError.value = 'La ville est obligatoire';
    return;
  }

  if (!companyForm.value.head_office.country || !companyForm.value.head_office.country.trim()) {
    companyFormError.value = 'Le pays est obligatoire';
    return;
  }

  try {
    savingCompany.value = true;

    // TODO: L'API de mise à jour n'existe pas encore
    // Pour l'instant, on affiche un message d'information
    const toast = useCustomToast();
    toast.info('La fonctionnalité de mise à jour des entreprises n\'est pas encore disponible. Les données ont été validées avec succès.');

    console.log('Données de l\'entreprise validées:', companyForm.value);
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
