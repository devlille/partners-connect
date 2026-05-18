<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Partenaires - {{ eventName }}</PageTitle>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/partners/create`"
          label="Créer un partenaire"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="3" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else>
        <!-- Compteurs par catégorie -->
        <div
          v-if="categories.length > 0"
          class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6"
        >
          <div
            v-for="category in categories"
            :key="`stat-${category.id}`"
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

        <!-- Vide -->
        <div
          v-if="partners.length === 0"
          class="bg-white rounded-lg shadow p-12 text-center text-gray-500"
        >
          Aucun partenaire pour cet événement pour le moment.
        </div>

        <!-- Liste des partenaires -->
        <div v-else>
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
                    Catégorie
                  </th>
                  <th
                    class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="partner in partners" :key="partner.id" class="hover:bg-gray-50">
                  <td
                    class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 cursor-pointer"
                    @click="onSelectPartner(partner)"
                  >
                    <div class="flex items-center gap-3">
                      <img
                        v-if="partner.company_logo_url"
                        :src="partner.company_logo_url"
                        :alt="`Logo ${partner.company_name}`"
                        class="h-8 w-8 rounded object-contain bg-gray-50"
                      />
                      <div
                        v-else
                        class="h-8 w-8 rounded bg-gray-100 flex items-center justify-center text-xs text-gray-400"
                      >
                        ?
                      </div>
                      <span>{{ partner.company_name }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {{ partner.category.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <UButton
                      color="error"
                      variant="ghost"
                      size="sm"
                      icon="i-heroicons-trash"
                      :loading="deletingPartnerId === partner.id"
                      @click.stop="confirmDelete(partner)"
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
                Êtes-vous sûr de vouloir supprimer le partenaire
                <strong>{{ partnerToDelete?.company_name }}</strong> ?
              </p>
              <p class="text-sm text-gray-500">
                Cette action est irréversible. L'entreprise liée et ses informations ne sont pas
                supprimées.
              </p>
            </div>

            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton
                color="neutral"
                variant="ghost"
                :disabled="!!deletingPartnerId"
                @click="isDeleteModalOpen = false"
              >
                Annuler
              </UButton>
              <UButton color="error" :loading="!!deletingPartnerId" @click="handleDelete">
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
  getEventBySlug,
  getOrgsEventsEcosystemPartnerCategories,
  getOrgsEventsEcosystemPartners,
  deleteOrgsEventsEcosystemPartner,
  type EcosystemPartnerCategory,
  type EcosystemPartnerItem,
} from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? (params[0] as string) : (params as string);
});
const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? (params[1] as string) : (params as string);
});

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>("");
const partners = ref<EcosystemPartnerItem[]>([]);
const categories = ref<EcosystemPartnerCategory[]>([]);

const isDeleteModalOpen = ref(false);
const partnerToDelete = ref<EcosystemPartnerItem | null>(null);
const deletingPartnerId = ref<string | null>(null);

function getCategoryCount(categoryId: string): number {
  return partners.value.filter((p) => p.category.id === categoryId).length;
}

function onSelectPartner(partner: EcosystemPartnerItem) {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/partners/${partner.id}`);
}

function confirmDelete(partner: EcosystemPartnerItem) {
  partnerToDelete.value = partner;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!partnerToDelete.value) return;
  try {
    deletingPartnerId.value = partnerToDelete.value.id;
    await deleteOrgsEventsEcosystemPartner(
      orgSlug.value,
      eventSlug.value,
      partnerToDelete.value.id,
    );
    await loadData();
    isDeleteModalOpen.value = false;
    partnerToDelete.value = null;
  } catch (err) {
    console.error("Failed to delete partner:", err);
    error.value = "Impossible de supprimer le partenaire.";
  } finally {
    deletingPartnerId.value = null;
  }
}

async function loadData() {
  try {
    loading.value = true;
    error.value = null;
    const [eventResponse, partnersResponse, categoriesResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsEcosystemPartners(orgSlug.value, eventSlug.value),
      getOrgsEventsEcosystemPartnerCategories(orgSlug.value, eventSlug.value),
    ]);
    eventName.value = eventResponse.data.event.name;
    partners.value = partnersResponse.data;
    categories.value = categoriesResponse.data;
  } catch (err) {
    console.error("Failed to load partners:", err);
    error.value = "Impossible de charger les partenaires.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});

watch([orgSlug, eventSlug], () => {
  loadData();
});

useHead({
  title: computed(() => `Partenaires - ${eventName.value || "Événement"} | DevLille`),
});
</script>
