<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/options`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Créer une option - {{ eventName }}</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-if="success" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
        Option créée avec succès ! Redirection en cours...
      </div>

      <SponsoringOptionForm
        :data="{}"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventBySlug, postOrgsEventsOptions, type CreateSponsoringOption } from "~/utils/api";
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

const error = ref<string | null>(null);
const success = ref(false);
const eventName = ref<string>('');

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

async function loadEventName() {
  try {
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;
  } catch (err) {
    console.error('Failed to load event:', err);
    error.value = 'Impossible de charger les informations de l\'événement';
  }
}

async function onSave(data: any) {
  try {
    error.value = null;
    success.value = false;

    // Le formulaire envoie { option: CreateSponsoringOption, selectedPacks: string[] }
    const optionData: CreateSponsoringOption = data.option;

    await postOrgsEventsOptions(orgSlug.value, eventSlug.value, optionData);
    success.value = true;

    setTimeout(() => {
      router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options`);
    }, 1000);
  } catch (err) {
    console.error('Failed to create option:', err);
    error.value = 'Impossible de créer l\'option. Vérifiez les données du formulaire.';
  }
}

onMounted(() => {
  loadEventName();
});

useHead({
  title: computed(() => `Créer une option - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
