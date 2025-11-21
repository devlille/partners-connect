<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
          <PageTitle>Prestataires - {{ eventName }}</PageTitle>
        </div>
        <UButton
          color="primary"
          size="lg"
          icon="i-heroicons-plus"
          @click="isAddModalOpen = true"
        >
          Ajouter un prestataire
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="5" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else-if="providers.length === 0" class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun prestataire</h3>
        <p class="mt-1 text-sm text-gray-500">Commencez par ajouter un prestataire.</p>
        <div class="mt-6">
          <UButton
            color="primary"
            icon="i-heroicons-plus"
            @click="isAddModalOpen = true"
          >
            Ajouter un prestataire
          </UButton>
        </div>
      </div>

      <div v-else class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nom</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Site web</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Téléphone</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="provider in providers" :key="provider.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ provider.name }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ provider.type }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                <a v-if="provider.website" :href="provider.website" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 underline">
                  {{ provider.website }}
                </a>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                <a v-if="provider.email" :href="`mailto:${provider.email}`" class="text-blue-600 hover:text-blue-800">
                  {{ provider.email }}
                </a>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ provider.phone || '-' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button
                  type="button"
                  :disabled="deletingId === provider.id"
                  class="text-red-600 hover:text-red-900 disabled:opacity-50 disabled:cursor-not-allowed"
                  @click="confirmDelete(provider)"
                >
                  <span v-if="deletingId === provider.id">Suppression...</span>
                  <span v-else>Supprimer</span>
                </button>
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
            <h3 class="text-lg font-semibold text-gray-900">Ajouter un prestataire</h3>
          </div>

          <div class="px-6 py-4">
            <form @submit.prevent="handleAddProvider" class="space-y-4">
              <div>
                <label for="name" class="block text-sm font-medium text-gray-700 mb-2">
                  Nom <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="name"
                  v-model="newProvider.name"
                  placeholder="Ex: Traiteur Dupont"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="type" class="block text-sm font-medium text-gray-700 mb-2">
                  Type <span class="text-red-500">*</span>
                </label>
                <UInput
                  id="type"
                  v-model="newProvider.type"
                  placeholder="Ex: Traiteur, Photographe, Décorateur..."
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="website" class="block text-sm font-medium text-gray-700 mb-2">
                  Site web (optionnel)
                </label>
                <UInput
                  id="website"
                  v-model="newProvider.website"
                  type="url"
                  placeholder="https://exemple.com"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="email" class="block text-sm font-medium text-gray-700 mb-2">
                  Email (optionnel)
                </label>
                <UInput
                  id="email"
                  v-model="newProvider.email"
                  type="email"
                  placeholder="contact@exemple.com"
                  :disabled="isSubmitting"
                  class="w-full"
                />
              </div>

              <div>
                <label for="phone" class="block text-sm font-medium text-gray-700 mb-2">
                  Téléphone (optionnel)
                </label>
                <UInput
                  id="phone"
                  v-model="newProvider.phone"
                  type="tel"
                  placeholder="+33 1 23 45 67 89"
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
              @click="handleAddProvider"
            >
              Ajouter
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal de suppression -->
    <Teleport to="body">
      <div v-if="isDeleteModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isDeleteModalOpen = false; providerToDelete = null">
        <div class="w-full max-w-md bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir supprimer le prestataire
              <strong>{{ providerToDelete?.name }}</strong> ?
            </p>
            <p class="mt-2 text-sm text-gray-500">
              Cette action est irréversible.
            </p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="deletingId !== null"
              @click="isDeleteModalOpen = false; providerToDelete = null"
            >
              Annuler
            </UButton>
            <UButton
              color="red"
              :loading="deletingId !== null"
              @click="handleDeleteConfirm"
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
import { getOrgsEventsProviders, postOrgsEventsProviders, postOrgsProviders, deleteOrgsEventsProviders, type ProviderSchema, type CreateProviderSchema } from '~/utils/api';
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

const providers = ref<ProviderSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref<string>('');

const isAddModalOpen = ref(false);
const newProvider = ref<Partial<CreateProviderSchema>>({
  name: '',
  type: '',
  website: '',
  email: '',
  phone: ''
});
const isSubmitting = ref(false);
const addError = ref<string | null>(null);

const isDeleteModalOpen = ref(false);
const providerToDelete = ref<ProviderSchema | null>(null);
const deletingId = ref<string | null>(null);

async function loadProviders() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getOrgsEventsProviders(orgSlug.value, eventSlug.value);
    providers.value = response.data.items;

    // TODO: Get event name from event API
    eventName.value = eventSlug.value;
  } catch (err) {
    console.error('Failed to load providers:', err);
    error.value = 'Impossible de charger les prestataires';
  } finally {
    loading.value = false;
  }
}

async function handleAddProvider() {
  addError.value = null;

  // Validation
  if (!newProvider.value.name || !newProvider.value.name.trim()) {
    addError.value = 'Le nom est obligatoire';
    return;
  }

  if (!newProvider.value.type || !newProvider.value.type.trim()) {
    addError.value = 'Le type est obligatoire';
    return;
  }

  // Validate website URL if provided
  if (newProvider.value.website && newProvider.value.website.trim()) {
    try {
      new URL(newProvider.value.website);
    } catch {
      addError.value = 'Le site web n\'est pas une URL valide';
      return;
    }
  }

  try {
    isSubmitting.value = true;

    // Étape 1: Créer le provider pour l'organisation
    const providerData: CreateProviderSchema = {
      name: newProvider.value.name!,
      type: newProvider.value.type!,
      website: newProvider.value.website || null,
      email: newProvider.value.email || null,
      phone: newProvider.value.phone || null
    };

    const createResponse = await postOrgsProviders(orgSlug.value, providerData);
    const providerId = createResponse.data.id;

    // Étape 2: Associer le provider à l'événement
    await postOrgsEventsProviders(orgSlug.value, eventSlug.value, [providerId]);

    // Recharger la liste
    await loadProviders();

    // Fermer le modal et réinitialiser
    isAddModalOpen.value = false;
    newProvider.value = {
      name: '',
      type: '',
      website: '',
      email: '',
      phone: ''
    };
  } catch (err) {
    console.error('Failed to add provider:', err);
    addError.value = 'Impossible d\'ajouter le prestataire. Vérifiez les informations.';
  } finally {
    isSubmitting.value = false;
  }
}

function confirmDelete(provider: ProviderSchema) {
  providerToDelete.value = provider;
  isDeleteModalOpen.value = true;
}

async function handleDeleteConfirm() {
  if (!providerToDelete.value) return;

  try {
    deletingId.value = providerToDelete.value.id;

    await deleteOrgsEventsProviders(
      orgSlug.value,
      eventSlug.value,
      [providerToDelete.value.id]
    );

    await loadProviders();

    isDeleteModalOpen.value = false;
    providerToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete provider:', err);
    error.value = 'Impossible de supprimer le prestataire';
  } finally {
    deletingId.value = null;
  }
}

onMounted(() => {
  loadProviders();
});

watch([orgSlug, eventSlug], () => {
  loadProviders();
});

useHead({
  title: computed(() => `Prestataires - ${eventName.value || 'Événement'} | DevLille`)
});
</script>
