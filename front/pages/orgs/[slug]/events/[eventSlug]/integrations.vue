<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <!-- Page Header -->
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>Intégrations</PageTitle>
          <p class="mt-1 text-sm text-gray-500">
            Gérez les intégrations de votre événement
          </p>
        </div>
        <UButton
          color="primary"
          size="lg"
          icon="i-heroicons-plus"
          @click="openCreateModal"
        >
          Ajouter une intégration
        </UButton>
      </div>
    </div>

    <!-- Page Content -->
    <div class="p-6">
      <!-- Error State (page level) -->
      <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
        {{ error }}
      </div>

      <!-- Integration List Component -->
      <IntegrationsIntegrationList
        :integrations="integrations"
        :loading="loading"
        :deleting-id="deletingId"
        @delete="confirmDelete"
      />
    </div>

    <!-- Create Integration Modal -->
    <IntegrationsIntegrationCreateModal
      :is-open="isCreateModalOpen"
      :configured-providers="configuredProviders"
      @close="isCreateModalOpen = false"
      @create="handleCreate"
    />

    <!-- Delete Integration Modal -->
    <IntegrationsIntegrationDeleteModal
      :is-open="isDeleteModalOpen"
      :integration="integrationToDelete"
      :deleting="deletingId !== null"
      @close="isDeleteModalOpen = false; integrationToDelete = null"
      @confirm="handleDeleteConfirm"
    />
  </Dashboard>
</template>

<script setup lang="ts">
import type { IntegrationSchema, IntegrationSchemaProvider, IntegrationSchemaUsage } from '~/utils/api';
import { useIntegrations } from '~/composables/useIntegrations';
import authMiddleware from '~/middleware/auth';

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

// Route parameters
const orgSlug = computed(() => {
  const params = route.params.slug;
  const slug = Array.isArray(params) ? params[0] as string : params as string;
  return slug.toLowerCase();
});

const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  const slug = Array.isArray(params) ? params[0] as string : params as string;
  return slug.toLowerCase();
});

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

// Integration management composable
const {
  integrations,
  loading,
  error,
  loadIntegrations,
  createIntegration,
  deleteIntegration,
  configuredProviders
} = useIntegrations({
  orgSlug: orgSlug.value,
  eventSlug: eventSlug.value
});

// Modal state for create
const isCreateModalOpen = ref(false);

// Modal state for delete
const isDeleteModalOpen = ref(false);
const integrationToDelete = ref<IntegrationSchema | null>(null);
const deletingId = ref<string | null>(null);

/**
 * Open create modal
 */
function openCreateModal() {
  isCreateModalOpen.value = true;
}

/**
 * Handle integration creation
 */
async function handleCreate(data: {
  provider: IntegrationSchemaProvider;
  usage: IntegrationSchemaUsage;
  configuration: Record<string, unknown>;
}) {
  try {
    await createIntegration(data);
    isCreateModalOpen.value = false;
  } catch (err) {
    console.error('Create error:', err);
    // Error is handled by composable and can be shown in modal
  }
}

/**
 * Open delete confirmation modal
 */
function confirmDelete(integration: IntegrationSchema) {
  integrationToDelete.value = integration;
  isDeleteModalOpen.value = true;
}

/**
 * Handle delete confirmation
 */
async function handleDeleteConfirm() {
  if (!integrationToDelete.value) return;

  try {
    deletingId.value = integrationToDelete.value.id;

    await deleteIntegration(integrationToDelete.value.id);

    isDeleteModalOpen.value = false;
    integrationToDelete.value = null;
  } catch (err) {
    console.error('Delete error:', err);
    // Error is handled by composable and displayed at page level
  } finally {
    deletingId.value = null;
  }
}

// Load integrations on mount
onMounted(() => {
  loadIntegrations();
});

// Page meta
useHead({
  title: computed(() => `Intégrations - ${eventSlug.value} | Partners Connect`)
});
</script>
