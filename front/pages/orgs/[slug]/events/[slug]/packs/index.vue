<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <h1 class="text-2xl font-bold text-gray-900">Packs de sponsoring - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/packs/create`"
          label="Créer un pack"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <div v-if="loading" class="flex justify-center py-8">
        <div class="text-gray-500">Chargement...</div>
      </div>

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="packs.length === 0" class="text-center py-12">
        <div class="text-gray-500 mb-4">Aucun pack créé pour le moment</div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/packs/create`"
          label="Créer votre premier pack"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="pack in packs"
          :key="pack.id"
          class="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow cursor-pointer"
          @click="navigateToPack(pack.id)"
        >
          <h3 class="text-lg font-semibold text-gray-900 mb-3">{{ pack.name }}</h3>
          <div class="space-y-2 text-sm text-gray-600">
            <p class="flex items-center justify-between">
              <span class="font-medium">Prix :</span>
              <span class="text-lg font-bold text-primary-600">{{ pack.base_price }}€</span>
            </p>
            <p class="flex items-center justify-between">
              <span class="font-medium">Options requises :</span>
              <span>{{ pack.required_options.length }}</span>
            </p>
            <p class="flex items-center justify-between">
              <span class="font-medium">Options facultatives :</span>
              <span>{{ pack.optional_options.length }}</span>
            </p>
            <p v-if="pack.max_quantity" class="flex items-center justify-between">
              <span class="font-medium">Quantité max :</span>
              <span>{{ pack.max_quantity }}</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPacks, getEventBySlug, type SponsoringPack } from "~/utils/api";
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
  const params = route.params.slug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const packs = ref<SponsoringPack[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des packs
const eventLinks = computed(() => [
  {
    label: 'Informations',
    icon: 'i-heroicons-information-circle',
    to: `/orgs/${orgSlug.value}/events/${eventSlug.value}`
  },
  {
    label: 'Mes Packs',
    icon: 'i-heroicons-cube',
    to: `/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`
  }
]);

const navigateToPack = (packId: string) => {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs/${packId}`);
};

async function loadPacks() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'événement
    const eventResponse = await getEventBySlug(eventSlug.value);
    eventName.value = eventResponse.data.event.name;

    // Charger les packs
    const response = await getOrgsEventsPacks(orgSlug.value, eventSlug.value);
    packs.value = response.data;
  } catch (err) {
    console.error('Failed to load packs:', err);
    error.value = 'Impossible de charger les packs';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPacks();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadPacks();
});

useHead({
  title: computed(() => `Packs de sponsoring - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
