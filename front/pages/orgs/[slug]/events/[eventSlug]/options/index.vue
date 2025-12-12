<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Options de sponsoring - {{ eventName }}</PageTitle>
        </div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
          icon="i-heroicons-plus"
          color="primary"
        >
          Créer une option
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="3" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else>
        <!-- Zone de recherche et filtres -->
        <div class="mb-6 bg-white rounded-lg shadow p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Recherche par libellé -->
            <div>
              <label for="search" class="block text-sm font-medium text-gray-700 mb-2">
                Rechercher par nom
              </label>
              <UInput
                id="search"
                v-model="searchQuery"
                type="text"
                placeholder="Ex: Logo, Stand, Tickets..."
                icon="i-heroicons-magnifying-glass"
                class="w-full"
              />
            </div>

            <!-- Filtre par type -->
            <div>
              <label for="type-filter" class="block text-sm font-medium text-gray-700 mb-2">
                Filtrer par type
              </label>
              <select
                id="type-filter"
                v-model="selectedType"
                class="w-full px-3 py-2 border border-gray-300 rounded-md bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">Tous les types</option>
                <option value="text">Texte libre</option>
                <option value="typed_quantitative">Quantité</option>
                <option value="typed_number">Nombre fixe</option>
                <option value="typed_selectable">Sélection</option>
              </select>
            </div>
          </div>

          <!-- Bouton de réinitialisation -->
          <div v-if="searchQuery || selectedType" class="mt-4">
            <UButton
              variant="ghost"
              color="neutral"
              size="sm"
              icon="i-heroicons-x-mark"
              @click="clearFilters"
            >
              Réinitialiser les filtres
            </UButton>
          </div>
        </div>

        <!-- Compteur de résultats -->
        <div class="text-sm text-gray-600 mb-4" role="status" aria-live="polite">
          {{ filteredOptions.length }} option(s) trouvée(s)
          <span v-if="searchQuery || selectedType" class="text-gray-500">
            (sur {{ options.length }} au total)
          </span>
        </div>

        <div v-if="filteredOptions.length === 0" class="text-center py-12">
          <div class="text-gray-500 mb-4">
            {{ searchQuery || selectedType ? 'Aucune option ne correspond à votre recherche' : 'Aucune option pour le moment' }}
          </div>
          <UButton
            v-if="!searchQuery && !selectedType"
            :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
            icon="i-heroicons-plus"
            color="primary"
          >
            Créer une option
          </UButton>
          <UButton
            v-else
            variant="ghost"
            color="neutral"
            icon="i-heroicons-x-mark"
            @click="clearFilters"
          >
            Réinitialiser les filtres
          </UButton>
        </div>

        <div v-else class="bg-white rounded-lg shadow overflow-hidden">
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
                  Type
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Prix
                </th>
                <th
                  class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Actions
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="option in filteredOptions" :key="option.id" class="hover:bg-gray-50">
                <td
                  class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 cursor-pointer"
                  @click="onRowClick(option)"
                >
                  {{ getOptionName(option) }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  <span :class="getTypeClass(option.type)">
                    {{ getTypeLabel(option.type) }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ formatPrice(option) }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <UButton
                    color="error"
                    variant="ghost"
                    size="sm"
                    icon="i-heroicons-trash"
                    :loading="deletingOptionId === option.id"
                    @click.stop="confirmDelete(option)"
                  >
                    Supprimer
                  </UButton>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>

    <!-- Modal de confirmation de suppression -->
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
              Êtes-vous sûr de vouloir supprimer l'option
              <strong>{{ optionToDelete ? getOptionName(optionToDelete) : '' }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">Cette action est irréversible.</p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!!deletingOptionId"
              @click="isDeleteModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton color="error" :loading="!!deletingOptionId" @click="handleDelete">
              Supprimer
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsOptions, getEventBySlug, deleteOrgsEventsOptions, type SponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
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

const options = ref<SponsoringOption[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

// Filtres
const searchQuery = ref('');
const selectedType = ref('');

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);
const { getOptionName } = useOptionTranslation();

const isDeleteModalOpen = ref(false);
const optionToDelete = ref<SponsoringOption | null>(null);
const deletingOptionId = ref<string | null>(null);

// Options filtrées
const filteredOptions = computed(() => {
  let filtered = options.value;

  // Filtre par recherche textuelle
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase().trim();
    filtered = filtered.filter(option => {
      const name = getOptionName(option).toLowerCase();
      return name.includes(query);
    });
  }

  // Filtre par type
  if (selectedType.value) {
    filtered = filtered.filter(option => option.type === selectedType.value);
  }

  return filtered;
});

// Fonction pour réinitialiser les filtres
function clearFilters() {
  searchQuery.value = '';
  selectedType.value = '';
}

async function loadOptions() {
  try {
    loading.value = true;
    error.value = null;

    const optionsResponse = await getOrgsEventsOptions(orgSlug.value, eventSlug.value)

    options.value = optionsResponse.data;
    eventName.value = "DevLille";
  } catch (err) {
    console.error('Failed to load options:', err);
    error.value = 'Impossible de charger les options';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOptions();
});

watch([orgSlug, eventSlug], () => {
  loadOptions();
});

function getTypeLabel(type: string): string {
  const typeLabels: Record<string, string> = {
    text: 'Texte libre',
    typed_quantitative: 'Quantité',
    typed_number: 'Nombre fixe',
    typed_selectable: 'Sélection'
  };
  return typeLabels[type] || type;
}

function getTypeClass(type: string): string {
  const defaultClass = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800';
  const typeClasses: Record<string, string> = {
    text: defaultClass,
    typed_quantitative: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800',
    typed_number: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800',
    typed_selectable: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800'
  };
  return typeClasses[type] || defaultClass;
}

function formatPrice(option: SponsoringOption): string {
  // Pour typed_selectable, afficher "Variable" car le prix dépend de la sélection
  if (option.type === 'typed_selectable') {
    return 'Variable';
  }
  // Pour typed_number, afficher le prix avec la quantité
  if (option.type === 'typed_number' && 'fixed_quantity' in option) {
    const price = option.price ? `${option.price}€` : 'Gratuit';
    return `${price} (×${option.fixed_quantity})`;
  }
  // Pour les autres types
  return option.price ? `${option.price}€` : 'Gratuit';
}

function onRowClick(row: SponsoringOption) {
  navigateTo(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options/${row.id}`);
}

function confirmDelete(option: SponsoringOption) {
  optionToDelete.value = option;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!optionToDelete.value) return;

  try {
    deletingOptionId.value = optionToDelete.value.id;

    await deleteOrgsEventsOptions(orgSlug.value, eventSlug.value, optionToDelete.value.id);

    // Recharger la liste
    await loadOptions();

    // Fermer le modal
    isDeleteModalOpen.value = false;
    optionToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete option:', err);
    error.value = 'Impossible de supprimer l\'option';
  } finally {
    deletingOptionId.value = null;
  }
}

useHead({
  title: computed(() => `Options - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
