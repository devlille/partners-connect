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
        <!-- Filter Panel -->
        <FilterPanel
          v-model="filters"
          :packs="packs"
          :loading="loading"
          :active-filter-count="activeFilterCount"
          @clear-all="clearAllFilters"
        />

        <!-- Active Filters Badges -->
        <ActiveFilters
          :filters="filters"
          :packs="packs"
          @clear="clearFilter"
          @clear-all="clearAllFilters"
        />

        <!-- Result Count with ARIA live region for screen readers -->
        <div
          v-if="!loading"
          class="text-sm text-gray-600 mb-4"
          role="status"
          aria-live="polite"
          aria-atomic="true"
        >
          {{ $t('sponsors.filters.showingResults', { count: partnerships.length }) }}
        </div>

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
          <div class="text-gray-500 mb-4">
            {{ activeFilterCount > 0 ? $t('sponsors.filters.noResults') : $t('sponsors.noSponsors') }}
          </div>
        </div>

        <div v-else>
          <UTable
            :data="partnerships"
            :columns="columns"
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
import { useSponsorFilters } from '~/composables/useSponsorFilters'
import FilterPanel from '~/components/sponsors/FilterPanel.vue'
import ActiveFilters from '~/components/sponsors/ActiveFilters.vue'

const { footerLinks } = useDashboardLinks();
const { getOrgSlug, getEventSlug } = useRouteParams();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => getOrgSlug());
const eventSlug = computed(() => getEventSlug());

// Initialize sponsor filters
const {
  filters,
  activeFilterCount,
  isEmpty: filtersEmpty,
  queryParams,
  clearAllFilters,
  clearFilter
} = useSponsorFilters({
  orgSlug: orgSlug.value,
  eventSlug: eventSlug.value
})

const columns = [
  {
    header: 'Nom de l\'entreprise',
    accessorKey: 'company_name',
    cell: (info: TableRow<PartnershipItem>) => info.getValue('company_name')
  },
  {
    header: 'Pack',
    accessorKey: 'pack_name',
    cell: (info: any) => {
      const packName = info.getValue('pack_name');
      const suggestedPackName = info.row.original.suggested_pack_name;

      if (packName) {
        return packName;
      } else if (suggestedPackName) {
        return `${suggestedPackName} (suggéré)`;
      }
      return '-';
    }
  },
  {
    header: 'Actions',
    accessorKey: 'id',
    cell: (info: any) => {
      const partnership = info.row.original;
      return h(resolveComponent('UButton'), {
        onClick: () => {
          navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnership.id}`);
        },
        icon: 'i-heroicons-arrow-right-circle',
        size: 'md',
        color: 'primary',
        variant: 'ghost',
        square: true,
        title: 'Voir le sponsor'
      });
    }
  }
];

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
    // Apply query params for filtering
    const [eventResponse, partnershipsResponse, packsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPartnership(orgSlug.value, eventSlug.value, queryParams.value),
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

// Watch for filter changes and reload partnerships
watchEffect(() => {
  // Trigger reload when queryParams change
  if (queryParams.value) {
    loadPartnerships();
  }
});

useHead({
  title: computed(() => `Sponsors - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
