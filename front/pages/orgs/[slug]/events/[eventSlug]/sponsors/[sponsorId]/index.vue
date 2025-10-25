<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors`" label="Retour aux sponsors" />
          <h1 class="text-2xl font-bold text-gray-900">{{ partnership?.company_name || 'Sponsor' }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="text-gray-500">Chargement...</div>
      </div>

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else>
        <!-- Tabs -->
        <div class="border-b border-gray-200 mb-6">
          <nav class="-mb-px flex space-x-8" aria-label="Tabs">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                activeTab === tab.id
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700',
                'whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium'
              ]"
            >
              {{ tab.label }}
            </button>
          </nav>
        </div>

        <!-- Partnership Tab -->
        <div v-show="activeTab === 'partnership'" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du partenariat</h2>
          <PartnershipForm
            :partnership="partnership"
            :loading="saving"
            @save="onSave"
            @cancel="onCancel"
          />
        </div>

        <!-- Company Info Tab -->
        <div v-show="activeTab === 'company'" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'entreprise</h2>

          <div v-if="loadingCompany" class="flex justify-center py-8">
            <div class="text-gray-500">Chargement...</div>
          </div>

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
                rows="4"
                class="w-full"
              />
            </div>

            <div class="border-t pt-4">
              <h3 class="text-sm font-medium text-gray-900 mb-3">Adresse du siège social</h3>

              <div class="space-y-4">
                <div>
                  <label for="company-street" class="block text-sm font-medium text-gray-700 mb-2">
                    Rue <span class="text-red-500">*</span>
                  </label>
                  <UInput
                    id="company-street"
                    v-model="companyForm.head_office.street"
                    placeholder="123 Rue de la République"
                    :disabled="savingCompany"
                    class="w-full"
                  />
                </div>

                <div>
                  <label for="company-street-2" class="block text-sm font-medium text-gray-700 mb-2">
                    Complément d'adresse (optionnel)
                  </label>
                  <UInput
                    id="company-street-2"
                    v-model="companyForm.head_office.street_2"
                    placeholder="Bâtiment A, 3ème étage"
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
                      v-model="companyForm.head_office.zip"
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
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getCompanies, getCompaniesPartnership, type PartnershipItem, type CompanySchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
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

const activeTab = ref<'partnership' | 'company'>('partnership');
const tabs = [
  { id: 'partnership' as const, label: 'Partenariat' },
  { id: 'company' as const, label: 'Entreprise' }
];

const partnership = ref<PartnershipItem | null>(null);
const loading = ref(true);
const saving = ref(false);
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
    street: '',
    street_2: '',
    city: '',
    zip: '',
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

    // Charger toutes les partnerships et trouver celle qui correspond à l'ID
    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const found = response.data.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;

    // Charger les informations de la company
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

    // Récupérer toutes les companies
    const companiesResponse = await getCompanies();
    const companies = companiesResponse.data.items;

    // Trouver la company qui a ce partnership
    for (const comp of companies) {
      const partnershipsResponse = await getCompaniesPartnership(comp.id);
      const hasPartnership = partnershipsResponse.data.find((p: any) => p.id === sponsorId.value);

      if (hasPartnership) {
        company.value = comp;

        // Initialiser le formulaire avec les données de la company
        companyForm.value = {
          name: comp.name,
          siret: comp.siret,
          vat: comp.vat,
          site_url: comp.site_url,
          description: comp.description || '',
          head_office: {
            street: comp.head_office.street,
            street_2: comp.head_office.street_2 || '',
            city: comp.head_office.city,
            zip: comp.head_office.zip,
            country: comp.head_office.country
          }
        };
        return;
      }
    }

    companyError.value = 'Entreprise non trouvée';
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
      street: company.value.head_office.street,
      street_2: company.value.head_office.street_2 || '',
      city: company.value.head_office.city,
      zip: company.value.head_office.zip,
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

  if (!companyForm.value.head_office.street || !companyForm.value.head_office.street.trim()) {
    companyFormError.value = 'La rue est obligatoire';
    return;
  }

  if (!companyForm.value.head_office.zip || !companyForm.value.head_office.zip.trim()) {
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

    // TODO: Appeler l'API de mise à jour quand elle sera disponible
    console.log('Données de la company à sauvegarder:', companyForm.value);

    // Pour l'instant, afficher un message
    companyFormError.value = 'La fonctionnalité de mise à jour sera disponible prochainement. Les données ont été validées avec succès.';

    // Simuler une mise à jour locale
    if (company.value) {
      company.value = {
        ...company.value,
        name: companyForm.value.name,
        siret: companyForm.value.siret,
        vat: companyForm.value.vat,
        site_url: companyForm.value.site_url,
        description: companyForm.value.description || null,
        head_office: {
          street: companyForm.value.head_office.street,
          street_2: companyForm.value.head_office.street_2 || null,
          city: companyForm.value.head_office.city,
          zip: companyForm.value.head_office.zip,
          country: companyForm.value.head_office.country
        }
      };
    }
  } catch (err) {
    console.error('Failed to save company:', err);
    companyFormError.value = 'Impossible de sauvegarder les modifications';
  } finally {
    savingCompany.value = false;
  }
}

async function onSave(data: any) {
  try {
    saving.value = true;
    error.value = null;

    // TODO: Appeler l'API de mise à jour quand elle sera disponible
    console.log('Données à sauvegarder:', data);

    // Pour l'instant, juste recharger les données
    await loadPartnership();

  } catch (err) {
    console.error('Failed to save partnership:', err);
    error.value = 'Impossible de sauvegarder les modifications';
  } finally {
    saving.value = false;
  }
}

function onCancel() {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors`);
}

onMounted(() => {
  loadPartnership();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
