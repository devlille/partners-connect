<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/options`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">{{ option?.name }} - {{ eventName }}</h1>
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

      <div v-else-if="option">
        <SponsoringOptionForm
          :data="optionFormData"
          @save="onSave"
        />
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, getOrgsEventsOptions, putOrgsEventsOptions, type SponsoringOption, type CreateSponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => route.params.slug as string);
const eventSlug = computed(() => route.params.eventSlug as string);
const optionId = computed(() => route.params.optionId as string);

const option = ref<SponsoringOption | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page d'édition d'option
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Convertir SponsoringOption en données pour le formulaire
const optionFormData = computed(() => {
  if (!option.value) return {};

  return {
    name: option.value.name,
    description: option.value.description || '',
    price: option.value.price || undefined
  };
});

async function loadOption() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger toutes les options et trouver celle qui correspond
    const optionsResponse = await getOrgsEventsOptions(orgSlug.value, eventSlug.value);
    const foundOption = optionsResponse.data.find(o => o.id === optionId.value);

    if (!foundOption) {
      error.value = 'Option non trouvée';
      return;
    }

    option.value = foundOption;
  } catch (err) {
    console.error('Failed to load option:', err);
    error.value = 'Impossible de charger les informations de l\'option';
  } finally {
    loading.value = false;
  }
}

async function onSave(optionData: CreateSponsoringOption) {
  try {
    error.value = null;

    await putOrgsEventsOptions(orgSlug.value, eventSlug.value, optionId.value, optionData);

    // Recharger les données après la mise à jour
    await loadOption();

    // Afficher un message de succès (vous pouvez ajouter un toast ici)
    console.log('Option mise à jour avec succès');

    // Rediriger vers la liste des options
    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options`);
    }, 500);
  } catch (err) {
    console.error('Failed to update option:', err);
    error.value = 'Impossible de mettre à jour l\'option';
  }
}

onMounted(() => {
  loadOption();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug, optionId], () => {
  loadOption();
});

useHead({
  title: computed(() => `${option.value?.name || 'Option'} - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
