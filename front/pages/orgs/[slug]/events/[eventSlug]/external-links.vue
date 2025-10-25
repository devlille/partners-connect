<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <h1 class="text-2xl font-bold text-gray-900">Liens externes - {{ eventName }}</h1>
        </div>
        <UButton
          color="primary"
          size="lg"
          icon="i-heroicons-plus"
          @click="isAddModalOpen = true"
        >
          Ajouter un lien
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="3" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="externalLinks.length === 0" class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun lien externe</h3>
        <p class="mt-1 text-sm text-gray-500">Commencez par ajouter un lien externe.</p>
        <div class="mt-6">
          <UButton
            color="primary"
            icon="i-heroicons-plus"
            @click="isAddModalOpen = true"
          >
            Ajouter un lien
          </UButton>
        </div>
      </div>

      <div v-else class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nom</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">URL</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="link in externalLinks" :key="link.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ link.name }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                <a :href="link.url" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 underline">
                  {{ link.url }}
                </a>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <UButton
                  color="error"
                  variant="ghost"
                  size="sm"
                  icon="i-heroicons-trash"
                  :loading="deletingLinkId === link.id"
                  @click="confirmDelete(link)"
                >
                  Supprimer
                </UButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal d'ajout -->
    <Teleport to="body">
      <div v-if="isAddModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isAddModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Ajouter un lien externe</h3>
          </div>

          <div class="px-6 py-4">
            <form @submit.prevent="handleAddLink" class="space-y-4">
              <div>
                <label for="name" class="block text-sm font-medium text-gray-700 mb-2">
                  Nom <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="name"
                  v-model="newLink.name"
                  placeholder="Ex: Site web, Billetterie, Programme..."
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="url" class="block text-sm font-medium text-gray-700 mb-2">
                  URL <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="url"
                  v-model="newLink.url"
                  type="url"
                  placeholder="https://exemple.com"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div v-if="addError" class="text-sm text-red-600">
                {{ addError }}
              </div>
            </form>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="isSubmitting"
              @click="isAddModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="isSubmitting"
              @click="handleAddLink"
            >
              Ajouter
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal de confirmation de suppression -->
    <Teleport to="body">
      <div v-if="isDeleteModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isDeleteModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir supprimer le lien <strong>{{ linkToDelete?.name }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">
              Cette action est irréversible.
            </p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!!deletingLinkId"
              @click="isDeleteModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="error"
              :loading="!!deletingLinkId"
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
import { getEventBySlug, postOrgsEventsExternalLink, deleteOrgsEventsExternalLink, type EventExternalLinkSchema, type CreateEventExternalLinkSchema } from '~/utils/api';
import authMiddleware from '~/middleware/auth';

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
  return Array.isArray(params) ? params[0] as string : params as string;
});

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

const externalLinks = ref<EventExternalLinkSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

const isAddModalOpen = ref(false);
const newLink = ref<Partial<CreateEventExternalLinkSchema>>({
  name: '',
  url: ''
});
const isSubmitting = ref(false);
const addError = ref<string | null>(null);

const isDeleteModalOpen = ref(false);
const linkToDelete = ref<EventExternalLinkSchema | null>(null);
const deletingLinkId = ref<string | null>(null);

async function loadExternalLinks() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getEventBySlug(eventSlug.value);
    eventName.value = response.data.event.name;
    externalLinks.value = response.data.event.external_links || [];
  } catch (err) {
    console.error('Failed to load external links:', err);
    error.value = 'Impossible de charger les liens externes';
  } finally {
    loading.value = false;
  }
}

async function handleAddLink() {
  addError.value = null;

  // Validation
  if (!newLink.value.name || !newLink.value.name.trim()) {
    addError.value = 'Le nom est obligatoire';
    return;
  }

  if (!newLink.value.url || !newLink.value.url.trim()) {
    addError.value = 'L\'URL est obligatoire';
    return;
  }

  // Validation basique de l'URL
  try {
    new URL(newLink.value.url);
  } catch {
    addError.value = 'L\'URL n\'est pas valide';
    return;
  }

  try {
    isSubmitting.value = true;

    const linkData: CreateEventExternalLinkSchema = {
      name: newLink.value.name!,
      url: newLink.value.url!
    };

    await postOrgsEventsExternalLink(orgSlug.value, eventSlug.value, linkData);

    // Recharger la liste
    await loadExternalLinks();

    // Fermer le modal et réinitialiser
    isAddModalOpen.value = false;
    newLink.value = {
      name: '',
      url: ''
    };
  } catch (err) {
    console.error('Failed to add external link:', err);
    addError.value = 'Impossible d\'ajouter le lien. Vérifiez les informations.';
  } finally {
    isSubmitting.value = false;
  }
}

function confirmDelete(link: EventExternalLinkSchema) {
  linkToDelete.value = link;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!linkToDelete.value) return;

  try {
    deletingLinkId.value = linkToDelete.value.id;

    await deleteOrgsEventsExternalLink(orgSlug.value, eventSlug.value, linkToDelete.value.id);

    // Recharger la liste
    await loadExternalLinks();

    // Fermer le modal
    isDeleteModalOpen.value = false;
    linkToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete external link:', err);
    error.value = 'Impossible de supprimer le lien';
  } finally {
    deletingLinkId.value = null;
  }
}

onMounted(() => {
  loadExternalLinks();
});

watch([orgSlug, eventSlug], () => {
  loadExternalLinks();
});

useHead({
  title: computed(() => `Liens externes - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
