<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Sponsors - {{ eventName }}</PageTitle>
        </div>
        <div class="flex gap-3">
          <UButton
            label="Envoyer un email"
            icon="i-heroicons-envelope"
            color="neutral"
            variant="outline"
            @click="showEmailModal = true"
          />
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/create`"
            label="Créer un sponsor"
            icon="i-heroicons-plus"
            color="primary"
          />
        </div>
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
          :metadata="filterMetadata"
          :loading="loading"
          :active-filter-count="activeFilterCount"
          @clear-all="clearAllFilters"
        />

        <!-- Active Filters Badges -->
        <ActiveFilters
          :filters="filters"
          :metadata="filterMetadata"
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
          {{ $t('sponsors.filters.showingResults', { count: totalItems }) }}
        </div>

        <!-- Statistiques -->
        <div
          v-if="packs.length > 0"
          class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6"
        >
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
          <UTable :data="partnerships" :columns="columns" />

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="flex items-center justify-between mt-6">
            <div class="text-sm text-gray-600">
              Page {{ currentPage }} sur {{ totalPages }} ({{ totalItems }} sponsors)
            </div>
            <div class="flex gap-2">
              <UButton
                icon="i-heroicons-chevron-left"
                color="neutral"
                variant="outline"
                size="sm"
                :disabled="currentPage <= 1"
                @click="goToPage(currentPage - 1)"
              />
              <UButton
                v-for="page in visiblePages"
                :key="page"
                :label="String(page)"
                :color="page === currentPage ? 'primary' : 'neutral'"
                :variant="page === currentPage ? 'solid' : 'outline'"
                size="sm"
                @click="goToPage(page)"
              />
              <UButton
                icon="i-heroicons-chevron-right"
                color="neutral"
                variant="outline"
                size="sm"
                :disabled="currentPage >= totalPages"
                @click="goToPage(currentPage + 1)"
              />
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Modale de confirmation de suppression -->
    <ConfirmModal
      v-model="showDeleteModal"
      :title="t('modals.confirmDelete')"
      :message="t('modals.irreversible')"
      type="danger"
      :confirm-label="t('common.delete')"
      :cancel-label="t('common.cancel')"
      :confirming="deleting"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />

    <!-- Annonce ARIA pour les lecteurs d'écran -->
    <div role="status" aria-live="polite" aria-atomic="true" class="sr-only">
      {{ ariaAnnouncement }}
    </div>

    <!-- Modale d'envoi d'email -->
    <SendEmailModal
      v-model="showEmailModal"
      :org-slug="orgSlug"
      :event-slug="eventSlug"
      :filter-params="emailFilterParams"
      :recipient-emails="recipientEmails"
      @sent="handleEmailSent"
    />
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership, getEventBySlug, getOrgsEventsPacks, deletePartnership, type PartnershipItemSchema, type SponsoringPack, type PostPartnershipEmailParams } from "~/utils/api";
import { filterStateToApiParams } from "~/types/sponsors";
import authMiddleware from "~/middleware/auth";
import { useSponsorFilters } from '~/composables/useSponsorFilters'
import FilterPanel from '~/components/sponsors/FilterPanel.vue'
import ActiveFilters from '~/components/sponsors/ActiveFilters.vue'

import type { PartnershipsMetadata } from '~/types/sponsors'

// Type pour la réponse paginée de l'API
interface PaginatedPartnershipsResponse {
  items: PartnershipItemSchema[];
  page: number;
  page_size: number;
  total: number;
  metadata?: PartnershipsMetadata;
}

const { t } = useI18n();
const { footerLinks } = useDashboardLinks();
const { getOrgSlug, getEventSlug } = useRouteParams();
const route = useRoute();
const router = useRouter();

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
    cell: (info: any) => {
      const partnership = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnership.id}`),
        class: 'cursor-pointer hover:underline'
      }, info.getValue('company_name'));
    }
  },
  {
    header: 'Pack',
    accessorKey: 'selected_pack_name',
    cell: (info: any) => {
      const partnership = info.row.original;
      const selectedPackName = info.getValue('selected_pack_name');
      const suggestedPackName = partnership.suggested_pack_name;

      let displayText = '-';
      if (selectedPackName) {
        displayText = selectedPackName;
      } else if (suggestedPackName) {
        displayText = `${suggestedPackName} (suggéré)`;
      }

      return h('div', {
        onClick: () => navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnership.id}`),
        class: 'cursor-pointer'
      }, displayText);
    }
  },
  {
    header: 'Organisateur',
    accessorKey: 'organiser',
    cell: (info: any) => {
      const partnership = info.row.original;
      const organiser = partnership.organiser;

      if (!organiser) {
        return h('div', {
          onClick: () => navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnership.id}`),
          class: 'cursor-pointer text-gray-400'
        }, '-');
      }

      return h('div', {
        onClick: () => navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors/${partnership.id}`),
        class: 'cursor-pointer text-sm'
      }, organiser.display_name || organiser.email);
    }
  },
  {
    header: t('common.actions'),
    accessorKey: 'actions',
    cell: (info: any) => {
      const partnership = info.row.original;
      return h('div', { class: 'flex gap-2' }, [
        h(resolveComponent('UButton'), {
          icon: 'i-heroicons-trash',
          color: 'error',
          variant: 'ghost',
          size: 'sm',
          onClick: (e: Event) => {
            e.stopPropagation();
            openDeleteModal(partnership);
          },
          'aria-label': t('aria.deletePartnership')
        })
      ]);
    }
  }
];

const partnerships = ref<PartnershipItemSchema[]>([]);
const packs = ref<SponsoringPack[]>([]);
const filterMetadata = ref<PartnershipsMetadata | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Pagination
const currentPage = ref(1);
const pageSize = ref(20);
const totalItems = ref(0);

const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value));

// Pages visibles pour la pagination (max 5 pages autour de la page courante)
const visiblePages = computed(() => {
  const pages: number[] = [];
  const total = totalPages.value;
  const current = currentPage.value;

  let start = Math.max(1, current - 2);
  let end = Math.min(total, current + 2);

  // Ajuster pour toujours afficher 5 pages si possible
  if (end - start < 4) {
    if (start === 1) {
      end = Math.min(total, start + 4);
    } else if (end === total) {
      start = Math.max(1, end - 4);
    }
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
});

function goToPage(page: number) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  loadPartnerships();
}

// Gestion de la suppression
const showDeleteModal = ref(false);
const partnershipToDelete = ref<PartnershipItemSchema | null>(null);
const deleting = ref(false);
const toast = useToast();

// Annonce ARIA pour les lecteurs d'écran
const ariaAnnouncement = ref('');

// Gestion de l'envoi d'email - synchronisé avec le query parameter 'email'
// On utilise un état interne qui attend que les données soient chargées
const emailModalInternal = ref(false);

// Computed pour lire/écrire dans l'URL
const showEmailModal = computed({
  get: () => emailModalInternal.value,
  set: (value: boolean) => {
    emailModalInternal.value = value;
    const query = { ...route.query };
    if (value) {
      query.email = 'true';
    } else {
      delete query.email;
    }
    router.replace({ query });
  }
});

// Ouvrir la modale depuis l'URL une fois les données chargées
watch(loading, (isLoading) => {
  if (!isLoading && route.query.email === 'true') {
    emailModalInternal.value = true;
  }
});

// Menu contextuel pour la page des sponsors
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Paramètres de filtre pour l'envoi d'email (convertis au format API)
const emailFilterParams = computed<PostPartnershipEmailParams>(() => {
  return filterStateToApiParams<PostPartnershipEmailParams>(filters.value);
});

// Liste des emails des destinataires (extraite des partenariats filtrés)
const recipientEmails = computed<string[]>(() => {
  const emails = new Set<string>();
  for (const partnership of partnerships.value) {
    if (partnership.emails) {
      // Le champ emails peut être une chaîne (séparée par virgules) ou un tableau
      if (typeof partnership.emails === 'string') {
        const partnerEmails = partnership.emails.split(',').map(e => e.trim()).filter(Boolean);
        partnerEmails.forEach(email => emails.add(email));
      } else if (Array.isArray(partnership.emails)) {
        partnership.emails.forEach(email => {
          if (email && typeof email === 'string') {
            emails.add(email.trim());
          }
        });
      }
    }
  }
  return Array.from(emails).sort();
});

// Gérer l'envoi d'email réussi
function handleEmailSent(recipientCount: number) {
  toast.add({
    title: 'Email envoyé',
    description: `L'email a été envoyé à ${recipientCount} destinataire(s).`,
    color: 'success'
  });
  ariaAnnouncement.value = `Email envoyé à ${recipientCount} destinataires`;
}

// Calculer le nombre de partnerships par pack
function getPackPartnershipCount(packId: string): number {
  // Trouver le nom du pack correspondant à l'ID
  const pack = packs.value.find(p => p.id === packId);
  if (!pack) return 0;

  // Compter les partnerships qui ont ce selected_pack_name
  return partnerships.value.filter(p => p.selected_pack_name === pack.name).length;
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

// Ouvrir la modale de suppression
function openDeleteModal(partnership: PartnershipItemSchema) {
  partnershipToDelete.value = partnership;
  showDeleteModal.value = true;
}

// Annuler la suppression
function cancelDelete() {
  showDeleteModal.value = false;
  partnershipToDelete.value = null;
}

// Confirmer la suppression
async function confirmDelete() {
  if (!partnershipToDelete.value) return;

  try {
    deleting.value = true;
    ariaAnnouncement.value = t('aria.deleting');

    await deletePartnership(
      orgSlug.value,
      eventSlug.value,
      partnershipToDelete.value.id
    );

    toast.add({
      title: t('success.title'),
      description: t('success.partnershipDeleted'),
      color: 'success'
    });

    // Annonce ARIA pour les lecteurs d'écran
    ariaAnnouncement.value = t('aria.partnershipDeleted');

    // Fermer la modale
    showDeleteModal.value = false;
    partnershipToDelete.value = null;

    // Recharger la liste
    await loadPartnerships();
  } catch (err: any) {
    console.error('Failed to delete partnership:', err);
    const errorMessage = err.response?.data?.message || t('errors.deletePartnership');

    toast.add({
      title: t('errors.title'),
      description: errorMessage,
      color: 'error'
    });

    ariaAnnouncement.value = `${t('errors.title')}: ${errorMessage}`;
  } finally {
    deleting.value = false;
  }
}

async function loadPartnerships() {
  try {
    loading.value = true;
    error.value = null;

    // Construire les paramètres avec pagination
    const paginationParams = {
      ...queryParams.value,
      page: currentPage.value,
      page_size: pageSize.value,
    };

    // Charger toutes les données en parallèle
    const [eventResponse, partnershipsResponse, packsResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPartnership(orgSlug.value, eventSlug.value, paginationParams),
      getOrgsEventsPacks(orgSlug.value, eventSlug.value)
    ]);

    eventName.value = eventResponse.data.event.name;

    // Gérer la nouvelle structure paginée de l'API
    const data = partnershipsResponse.data as unknown as PaginatedPartnershipsResponse;
    partnerships.value = data.items;
    totalItems.value = data.total;
    filterMetadata.value = data.metadata || null;

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

// Watch for filter changes and reload partnerships (reset to page 1)
watch(
  () => queryParams.value,
  () => {
    currentPage.value = 1;
    loadPartnerships();
  },
  { deep: true }
);

useHead({
  title: computed(() => `Sponsors - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
