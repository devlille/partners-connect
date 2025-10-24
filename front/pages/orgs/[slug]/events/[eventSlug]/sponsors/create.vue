<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors`" label="Retour aux sponsors" />
          <h1 class="text-2xl font-bold text-gray-900">Créer un sponsor - {{ eventName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="text-gray-500">Chargement...</div>
      </div>

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-else-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
        Sponsor créé avec succès ! Redirection en cours...
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du sponsor</h2>

        <form @submit.prevent="onSave" class="space-y-6">
          <!-- Company Information -->
          <div>
            <h3 class="text-md font-semibold text-gray-900 mb-4">Informations de l'entreprise</h3>
            <div class="grid grid-cols-1 gap-4">
              <div>
                <label for="company_name" class="block text-sm font-medium text-gray-700 mb-1">Nom de l'entreprise*</label>
                <UInput
                  id="company_name"
                  v-model="formData.company_name"
                  type="text"
                  required
                  placeholder="Nom de l'entreprise"
                />
              </div>

              <div>
                <label for="contact_name" class="block text-sm font-medium text-gray-700 mb-1">Nom du contact*</label>
                <UInput
                  id="contact_name"
                  v-model="formData.contact_name"
                  type="text"
                  required
                  placeholder="Nom du contact"
                />
              </div>

              <div>
                <label for="contact_role" class="block text-sm font-medium text-gray-700 mb-1">Rôle du contact</label>
                <UInput
                  id="contact_role"
                  v-model="formData.contact_role"
                  type="text"
                  placeholder="Ex: Directeur Marketing"
                />
              </div>

              <div>
                <label for="email" class="block text-sm font-medium text-gray-700 mb-1">Email*</label>
                <UInput
                  id="email"
                  v-model="formData.email"
                  type="email"
                  required
                  placeholder="email@entreprise.com"
                />
              </div>

              <div>
                <label for="phone" class="block text-sm font-medium text-gray-700 mb-1">Téléphone</label>
                <UInput
                  id="phone"
                  v-model="formData.phone"
                  type="tel"
                  placeholder="+33 6 12 34 56 78"
                />
              </div>
            </div>
          </div>

          <!-- Pack Selection -->
          <div class="border-t pt-6">
            <h3 class="text-md font-semibold text-gray-900 mb-4">Pack de sponsoring</h3>
            <div>
              <label for="pack" class="block text-sm font-medium text-gray-700 mb-1">Sélectionner un pack*</label>
              <select
                id="pack"
                v-model="formData.pack_id"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">-- Choisir un pack --</option>
                <option v-for="pack in packs" :key="pack.id" :value="pack.id">
                  {{ pack.name }} - {{ pack.base_price }}€
                </option>
              </select>
            </div>
          </div>

          <!-- Optional Options -->
          <div v-if="availableOptions.length > 0" class="border-t pt-6">
            <h3 class="text-md font-semibold text-gray-900 mb-4">Options optionnelles</h3>
            <div class="space-y-2">
              <div v-for="option in availableOptions" :key="option.id" class="flex items-center space-x-3">
                <input
                  :id="`option-${option.id}`"
                  v-model="formData.option_ids"
                  type="checkbox"
                  :value="option.id"
                  class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                />
                <label :for="`option-${option.id}`" class="text-sm text-gray-700 cursor-pointer">
                  {{ getOptionName(option) }} ({{ option.price || 0 }}€)
                </label>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-4 pt-4">
            <UButton
              type="button"
              color="gray"
              variant="outline"
              @click="onCancel"
            >
              Annuler
            </UButton>
            <UButton
              type="submit"
              color="primary"
              :loading="saving"
            >
              Créer le sponsor
            </UButton>
          </div>
        </form>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, getOrgsEventsPacks, postCompanies, postEventsPartnership, type SponsoringPack, type SponsoringOption, type CreateCompany, type RegisterPartnership } from "~/utils/api";
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

const packs = ref<SponsoringPack[]>([]);
const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const success = ref(false);
const eventName = ref<string>('');

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const formData = ref({
  company_name: '',
  contact_name: '',
  contact_role: '',
  email: '',
  phone: '',
  pack_id: '',
  option_ids: [] as string[],
});

// Options disponibles en fonction du pack sélectionné
const availableOptions = computed(() => {
  if (!formData.value.pack_id) return [];
  const selectedPack = packs.value.find(p => p.id === formData.value.pack_id);
  return selectedPack?.optional_options || [];
});

// Réinitialiser les options quand on change de pack
watch(() => formData.value.pack_id, () => {
  formData.value.option_ids = [];
});

function getOptionName(option: SponsoringOption): string {
  // Récupérer la première traduction disponible
  if (option.translations) {
    const firstTranslation = Object.values(option.translations)[0];
    if (firstTranslation && typeof firstTranslation === 'object' && 'name' in firstTranslation) {
      return firstTranslation.name as string;
    }
  }
  return option.name || 'Option sans nom';
}

async function loadData() {
  try {
    loading.value = true;
    const [eventResponse, packsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPacks(orgSlug.value, eventSlug.value)
    ]);
    eventName.value = eventResponse.data.event.name;
    packs.value = packsResponse.data;
  } catch (err) {
    console.error('Failed to load data:', err);
    error.value = 'Impossible de charger les informations';
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  try {
    saving.value = true;
    error.value = null;

    // Step 1: Create company
    const companyData: CreateCompany = {
      name: formData.value.company_name,
      head_office: {
        address: '',
        city: '',
        zip_code: '',
        country: '',
      },
      siret: '',
      vat: '',
      site_url: '',
    };

    const companyResponse = await postCompanies(companyData);

    // Step 2: Create partnership
    const partnershipData: RegisterPartnership = {
      company_id: companyResponse.data.id,
      pack_id: formData.value.pack_id,
      option_ids: formData.value.option_ids,
      contact_name: formData.value.contact_name,
      contact_role: formData.value.contact_role,
      language: 'fr',
      phone: formData.value.phone || null,
      emails: formData.value.email ? [formData.value.email] : [],
    };

    await postEventsPartnership(eventSlug.value, partnershipData);
    success.value = true;

    // Rediriger vers la liste des sponsors après 1 seconde
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors`);
    }, 1000);
  } catch (err) {
    console.error('Failed to create sponsor:', err);
    error.value = 'Impossible de créer le sponsor. Vérifiez les données du formulaire.';
  } finally {
    saving.value = false;
  }
}

function onCancel() {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors`);
}

onMounted(() => {
  loadData();
});

useHead({
  title: computed(() => `Créer un sponsor - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
