<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Packs de sponsoring - {{ eventName }}</PageTitle>
        </div>
        <div class="flex gap-3">
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/packs/categories/create`"
            label="Créer une catégorie"
            icon="i-heroicons-tag"
            color="neutral"
            variant="outline"
          />
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}/packs/create`"
            label="Créer un pack"
            icon="i-heroicons-plus"
            color="primary"
          />
        </div>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="3" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else>
        <!-- Statistiques -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
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
                {{ getPackCount(pack.id) }}
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
                  :class="getProgressBarColor(getPackCount(pack.id), pack.max_quantity)"
                  :style="{ width: `${getProgressPercentage(getPackCount(pack.id), pack.max_quantity)}%` }"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Liste des packs -->
        <div>
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Nom
                  </th>
                  <th
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Prix
                  </th>
                  <th
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Quantité max
                  </th>
                  <th
                    class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="pack in packs" :key="pack.id" class="hover:bg-gray-50">
                  <td
                    class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 cursor-pointer"
                    @click="onSelectPack(pack)"
                  >
                    {{ pack.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {{ pack.base_price }}€
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {{ pack.max_quantity || '-' }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <UButton
                      color="error"
                      variant="ghost"
                      size="sm"
                      icon="i-heroicons-trash"
                      :loading="deletingPackId === pack.id"
                      @click.stop="confirmDelete(pack)"
                    >
                      Supprimer
                    </UButton>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Catégories de partenaires - compteurs -->
        <div
          v-if="categories.length > 0"
          class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6 mt-10"
        >
          <div
            v-for="category in categories"
            :key="`stat-category-${category.id}`"
            class="bg-white rounded-lg shadow p-6 border border-gray-200"
          >
            <div class="flex items-center justify-between mb-2">
              <h3 class="text-sm font-medium text-gray-500">{{ category.name }}</h3>
            </div>
            <div class="flex items-baseline gap-2">
              <span class="text-3xl font-bold text-gray-900">
                {{ getCategoryCount(category.id) }}
              </span>
              <span class="text-sm text-gray-500">partenaire(s)</span>
            </div>
          </div>
        </div>

        <!-- Liste des catégories de partenaires -->
        <div v-if="categories.length > 0">
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Nom
                  </th>
                  <th
                    class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="category in categories" :key="category.id" class="hover:bg-gray-50">
                  <td
                    class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 cursor-pointer"
                    @click="onSelectCategory(category)"
                  >
                    {{ category.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <UButton
                      color="error"
                      variant="ghost"
                      size="sm"
                      icon="i-heroicons-trash"
                      :loading="deletingCategoryId === category.id"
                      @click.stop="confirmDeleteCategory(category)"
                    >
                      Supprimer
                    </UButton>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>

    <!-- Modal de confirmation de suppression -->
    <ClientOnly>
      <Teleport to="body">
        <div
          v-if="isDeleteModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
          @click.self="isDeleteModalOpen = false"
        >
          <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
            <div class="px-6 py-4 border-b border-gray-200">
              <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
            </div>

            <div class="px-6 py-4 space-y-4">
              <p class="text-sm text-gray-700">
                Êtes-vous sûr de vouloir supprimer le pack
                <strong>{{ packToDelete?.name }}</strong> ?
              </p>
              <p class="text-sm text-gray-500">
                Cette action est irréversible et supprimera également toutes les options associées.
              </p>
            </div>

            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton
                color="neutral"
                variant="ghost"
                :disabled="!!deletingPackId"
                @click="isDeleteModalOpen = false"
              >
                Annuler
              </UButton>
              <UButton color="error" :loading="!!deletingPackId" @click="handleDelete">
                Supprimer
              </UButton>
            </div>
          </div>
        </div>
      </Teleport>
    </ClientOnly>

    <!-- Modal de confirmation de suppression d'une catégorie -->
    <ClientOnly>
      <Teleport to="body">
        <div
          v-if="isDeleteCategoryModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
          @click.self="isDeleteCategoryModalOpen = false"
        >
          <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
            <div class="px-6 py-4 border-b border-gray-200">
              <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
            </div>

            <div class="px-6 py-4 space-y-4">
              <p class="text-sm text-gray-700">
                Êtes-vous sûr de vouloir supprimer la catégorie
                <strong>{{ categoryToDelete?.name }}</strong> ?
              </p>
              <p class="text-sm text-gray-500">
                Cette action est irréversible. Elle échouera si des partenaires utilisent encore
                cette catégorie.
              </p>
            </div>

            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton
                color="neutral"
                variant="ghost"
                :disabled="!!deletingCategoryId"
                @click="isDeleteCategoryModalOpen = false"
              >
                Annuler
              </UButton>
              <UButton color="error" :loading="!!deletingCategoryId" @click="handleDeleteCategory">
                Supprimer
              </UButton>
            </div>
          </div>
        </div>
      </Teleport>
    </ClientOnly>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getOrgsEventsPacks,
  getEventBySlug,
  deleteOrgsEventsPacks,
  getOrgsEventsPartnership,
  getOrgsEventsEcosystemPartnerCategories,
  getOrgsEventsEcosystemPartners,
  deleteOrgsEventsEcosystemPartnerCategory,
  type SponsoringPack,
  type PaginationMetadataSchemaPackCountsItem,
  type EcosystemPartnerCategory,
  type EcosystemPartnerItem,
} from "~/utils/api";
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
const packCounts = ref<PaginationMetadataSchemaPackCountsItem[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Menu contextuel pour la page des packs
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const isDeleteModalOpen = ref(false);
const packToDelete = ref<SponsoringPack | null>(null);
const deletingPackId = ref<string | null>(null);

const categories = ref<EcosystemPartnerCategory[]>([]);
const partners = ref<EcosystemPartnerItem[]>([]);
const isDeleteCategoryModalOpen = ref(false);
const categoryToDelete = ref<EcosystemPartnerCategory | null>(null);
const deletingCategoryId = ref<string | null>(null);

function getCategoryCount(categoryId: string): number {
  return partners.value.filter((p) => p.category.id === categoryId).length;
}

function onSelectCategory(category: EcosystemPartnerCategory) {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs/categories/${category.id}`);
}

function confirmDeleteCategory(category: EcosystemPartnerCategory) {
  categoryToDelete.value = category;
  isDeleteCategoryModalOpen.value = true;
}

async function handleDeleteCategory() {
  if (!categoryToDelete.value) return;
  try {
    deletingCategoryId.value = categoryToDelete.value.id;
    await deleteOrgsEventsEcosystemPartnerCategory(
      orgSlug.value,
      eventSlug.value,
      categoryToDelete.value.id,
    );
    await loadPacks();
    isDeleteCategoryModalOpen.value = false;
    categoryToDelete.value = null;
  } catch (err) {
    console.error("Failed to delete category:", err);
    error.value =
      "Impossible de supprimer la catégorie. Elle est peut-être encore utilisée par un partenaire.";
  } finally {
    deletingCategoryId.value = null;
  }
}

// Obtenir le nombre de partnerships pour un pack depuis les métadonnées
function getPackCount(packId: string): number {
  return packCounts.value.find(pc => pc.pack_id === packId)?.count ?? 0;
}

// Calculer le pourcentage de remplissage
function getProgressPercentage(count: number, maxQuantity: number): number {
  return Math.min((count / maxQuantity) * 100, 100);
}

// Déterminer la couleur de la barre de progression
function getProgressBarColor(count: number, maxQuantity: number): string {
  const percentage = getProgressPercentage(count, maxQuantity);
  if (percentage >= 90) return 'bg-red-500';
  if (percentage >= 70) return 'bg-yellow-500';
  return 'bg-green-500';
}

const onSelectPack = (row: SponsoringPack) => {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs/${row.id}`);
};

function confirmDelete(pack: SponsoringPack) {
  packToDelete.value = pack;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!packToDelete.value) return;

  try {
    deletingPackId.value = packToDelete.value.id;

    await deleteOrgsEventsPacks(orgSlug.value, eventSlug.value, packToDelete.value.id);

    // Recharger la liste
    await loadPacks();

    // Fermer le modal
    isDeleteModalOpen.value = false;
    packToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete pack:', err);
    error.value = 'Impossible de supprimer le pack';
  } finally {
    deletingPackId.value = null;
  }
}

async function loadPacks() {
  try {
    loading.value = true;
    error.value = null;

    // Charger toutes les données en parallèle
    const [
      eventResponse,
      packsResponse,
      partnershipsResponse,
      categoriesResponse,
      partnersResponse,
    ] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsPacks(orgSlug.value, eventSlug.value),
      getOrgsEventsPartnership(orgSlug.value, eventSlug.value),
      getOrgsEventsEcosystemPartnerCategories(orgSlug.value, eventSlug.value),
      getOrgsEventsEcosystemPartners(orgSlug.value, eventSlug.value),
    ]);

    eventName.value = eventResponse.data.event.name;
    packs.value = packsResponse.data;

    // Extraire les pack_counts depuis les métadonnées de la réponse paginée
    const partnershipsData = partnershipsResponse.data as unknown as { metadata?: { pack_counts?: PaginationMetadataSchemaPackCountsItem[] } };
    packCounts.value = partnershipsData.metadata?.pack_counts ?? [];

    categories.value = categoriesResponse.data;
    partners.value = partnersResponse.data;
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
