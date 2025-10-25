<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Options de sponsoring - {{ eventName }}</h1>
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

      <div v-else-if="options.length === 0" class="text-center py-12">
        <div class="text-gray-500 mb-4">Aucune option pour le moment</div>
        <UButton
          :to="`/orgs/${orgSlug}/events/${eventSlug}/options/create`"
          icon="i-heroicons-plus"
          color="primary"
        >
          Créer une option
        </UButton>
      </div>

      <div v-else class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nom</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Prix</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="option in options" :key="option.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 cursor-pointer" @click="onRowClick(option)">
                {{ getOptionName(option) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ option.price ? `${option.price}€` : 'Gratuit' }}
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
    </div>

    <!-- Modal de confirmation de suppression -->
    <Teleport to="body">
      <div v-if="isDeleteModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isDeleteModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir supprimer l'option <strong>{{ optionToDelete ? getOptionName(optionToDelete) : '' }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">
              Cette action est irréversible.
            </p>
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
            <UButton
              color="error"
              :loading="!!deletingOptionId"
              @click="handleDelete"
            >
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

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);
const { getOptionName } = useOptionTranslation();

const isDeleteModalOpen = ref(false);
const optionToDelete = ref<SponsoringOption | null>(null);
const deletingOptionId = ref<string | null>(null);

async function loadOptions() {
  try {
    loading.value = true;
    error.value = null;

    const [optionsResponse, eventResponse] = await Promise.all([
      getOrgsEventsOptions(orgSlug.value, eventSlug.value),
      getEventBySlug(eventSlug.value)
    ]);

    options.value = optionsResponse.data;
    eventName.value = eventResponse.data.event.name;
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
