<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Sponsors - {{ eventName }}</h1>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/create`"
          label="Créer un sponsor"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="2" :rows="10" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else>
        <!-- Statistiques -->
        <div v-if="packs.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
          <div
            v-for="pack in packs"
            :key="`stat-${pack.id}`"
            class="bg-white rounded-lg shadow p-6 border border-gray-200"
          >
            <div class="flex items-center justify-between mb-2">
              <h3 class="text-sm font-medium text-gray-500">{{ pack.name }}</h3>
              <div class="text-xs text-gray-400">
                {{ pack.max_quantity ? `Max: ${pack.max_quantity}` : 'Illimité' }}
              </div>
            </div>
            <div class="flex items-baseline gap-2">
              <span class="text-3xl font-bold text-gray-900">
                {{ getPackPartnershipCount(pack.id) }}
              </span>
              <span v-if="pack.max_quantity" class="text-sm text-gray-500">
                / {{ pack.max_quantity }}
              </span>
              <span class="text-sm text-gray-500">sponsor(s)</span>
            </div>
            <div v-if="pack.max_quantity" class="mt-3">
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="h-2 rounded-full transition-all"
                  :class="getProgressBarColor(pack.id, pack.max_quantity)"
                  :style="{ width: `${getProgressPercentage(pack.id, pack.max_quantity)}%` }"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Liste des sponsors -->
        <div v-if="partnerships.length === 0" class="text-center py-12">
          <div class="text-gray-500 mb-4">Aucun sponsor pour le moment</div>
        </div>

        <div v-else>
          <UTable
            :data="partnerships"
            :columns="columns"
            @select="onRowClick"
          />
        </div>
      </template>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getEventBySlug, getOrgsEventsPacks, type PartnershipItem, type SponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type {TableRow} from "@nuxt/ui";

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const columns = [
  {
    header: 'Nom de l\'entreprise',
    accessorKey: 'company_name',
    cell: (info: TableRow<PartnershipItem>) => info.getValue('company_name')
  },
  {
    header: 'Pack',
    accessorKey: 'pack_name',
    cell: (info: TableRow<PartnershipItem>) => {
      const packName = info.getValue('pack_name');
      const suggestedPackName = info.row.original.suggested_pack_name;

      if (packName) {
        return packName;
      } else if (suggestedPackName) {
        return `${suggestedPackName} (suggéré)`;
      }
      return '-';
    }
  }
];

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] as string : params as string;
});
const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] as string : params as string;
});

const partnerships = ref<PartnershipItem[]>([]);
const packs = ref<SponsoringPack[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des sponsors
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Calculer le nombre de partnerships par pack
function getPackPartnershipCount(packId: string): number {
  // Trouver le nom du pack correspondant à l'ID
  const pack = packs.value.find(p => p.id === packId);
  if (!pack) return 0;

  // Compter les partnerships qui ont ce pack_name
  return partnerships.value.filter(p => p.pack_name === pack.name).length;
}

// Calculer le pourcentage de remplissage
function getProgressPercentage(packId: string, maxQuantity: number): number {
  const count = getPackPartnershipCount(packId);
  return Math.min((count / maxQuantity) * 100, 100);
}

// Déterminer la couleur de la barre de progression
function getProgressBarColor(packId: string, maxQuantity: number): string {
  const percentage = getProgressPercentage(packId, maxQuantity);
  if (percentage >= 90) return 'bg-red-500';
  if (percentage >= 70) return 'bg-yellow-500';
  return 'bg-green-500';
}

async function loadPartnerships() {
  try {
    loading.value = true;
    error.value = null;

    // Charger toutes les données en parallèle
    const [eventResponse, partnershipsResponse, packsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPartnership(orgSlug.value, eventSlug.value),
      getOrgsEventsPacks(orgSlug.value, eventSlug.value)
    ]);

    eventName.value = eventResponse.data.event.name;
    partnerships.value = partnershipsResponse.data;
    packs.value = packsResponse.data;
  } catch (err) {
    console.error('Failed to load partnerships:', err);
    error.value = 'Impossible de charger les sponsors';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPartnerships();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug], () => {
  loadPartnerships();
});

// Gérer le clic sur une ligne du tableau
function onRowClick(row: TableRow<PartnershipItem>) {
  navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${row.original.id}`);
}

useHead({
  title: computed(() => `Sponsors - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
