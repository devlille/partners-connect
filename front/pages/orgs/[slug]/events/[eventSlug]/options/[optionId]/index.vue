<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/options`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">{{ option?.name || 'Option' }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="option" class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'option</h2>
        <SponsoringOptionForm
          :data="optionFormData"
          @save="onSave"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsOptions, putOrgsEventsOptions, type SponsoringOption } from "~/utils/api";
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

const optionId = computed(() => {
  const params = route.params.optionId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const option = ref<SponsoringOption | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Préparer les données pour le formulaire
const optionFormData = computed(() => {
  if (!option.value) return {};

  // L'API retourne translations.fr.name, pas name directement
  const frTranslation = option.value.translations?.fr;

  return {
    name: frTranslation?.name || '',
    description: frTranslation?.description || '',
    price: option.value.price || 0
  };
});

async function loadOption() {
  try {
    loading.value = true;
    error.value = null;

    // Charger toutes les options et trouver celle qui correspond
    const response = await getOrgsEventsOptions(orgSlug.value, eventSlug.value);
    const found = response.data.find(o => o.id === optionId.value);

    if (!found) {
      error.value = 'Option non trouvée';
      return;
    }

    option.value = found;
  } catch (err) {
    console.error('Failed to load option:', err);
    error.value = 'Impossible de charger l\'option';
  } finally {
    loading.value = false;
  }
}

async function onSave(data: any) {
  try {
    error.value = null;

    // Le formulaire envoie { option: CreateSponsoringOption, selectedPacks: string[] }
    const optionData = data.option;

    await putOrgsEventsOptions(orgSlug.value, eventSlug.value, optionId.value, optionData);

    // Rediriger vers la liste
    router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options`);
  } catch (err) {
    console.error('Failed to update option:', err);
    error.value = 'Impossible de mettre à jour l\'option';
  }
}

onMounted(() => {
  loadOption();
});

watch([orgSlug, eventSlug, optionId], () => {
  loadOption();
});

useHead({
  title: computed(() => `${option.value?.name || 'Option'} | DevLille`)
});
</script>
