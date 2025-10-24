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

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du sponsor</h2>
        <PartnershipForm
          :partnership="partnership"
          :loading="saving"
          @save="onSave"
          @cancel="onCancel"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, type PartnershipItem } from "~/utils/api";
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

const partnership = ref<PartnershipItem | null>(null);
const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);

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
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
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
