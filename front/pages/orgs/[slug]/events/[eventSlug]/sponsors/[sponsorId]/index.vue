<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ partnership?.company_name || 'Sponsor' }}</h1>
          <p class="text-sm text-gray-600 mt-1">Partenariat</p>
        </div>
        <div v-if="partnership" class="flex gap-3">
          <UButton
            color="error"
            variant="outline"
            :loading="isDeclining"
            :disabled="isValidating"
            :aria-label="`Refuser le partenariat avec ${partnership.company_name}`"
            @click="handleDeclinePartnership"
          >
            <i class="i-heroicons-x-mark mr-2" aria-hidden="true" />
            Refuser
          </UButton>
          <UButton
            color="primary"
            :loading="isValidating"
            :disabled="isDeclining"
            :aria-label="`Valider le partenariat avec ${partnership.company_name}`"
            @click="handleValidatePartnership"
          >
            <i class="i-heroicons-check mr-2" aria-hidden="true" />
            Valider
          </UButton>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du partenariat</h2>
        <PartnershipForm
          :partnership="partnership"
          :loading="saving"
          @save="onSave"
          @cancel="onCancel"
        />
      </div>
    </div>

    <!-- Modale de confirmation globale -->
    <ConfirmModal
      v-model="confirmState.isOpen"
      :title="confirmState.options.title"
      :message="confirmState.options.message"
      :confirm-label="confirmState.options.confirmLabel"
      :cancel-label="confirmState.options.cancelLabel"
      :type="confirmState.options.type"
      :confirming="confirmState.confirming"
      @confirm="handleConfirm"
      @cancel="handleCancel"
    />
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsPartnership } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";
import { PARTNERSHIP_CONFIRM } from "~/constants/partnership";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();
const { validatePartnership, declinePartnership } = usePartnershipActions();
const { confirm, confirmState, handleConfirm: confirmModalConfirm, handleCancel: confirmModalCancel } = useConfirm();

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

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);

// États pour validation/refus de partenariat
const isValidating = ref(false);
const isDeclining = ref(false);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    // Charger toutes les partnerships et trouver celle qui correspond à l'ID
    const response = await getOrgsEventsPartnership(orgSlug.value, eventSlug.value);
    const found = response.data.find(p => p.id === sponsorId.value);

    if (!found) {
      error.value = 'Sponsor non trouvé';
      return;
    }

    partnership.value = found;
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

async function onSave(data: any) {
  try {
    saving.value = true;
    error.value = null;

    // TODO: Appeler l'API de mise à jour quand elle sera disponible
    console.log('Données à sauvegarder:', data);

    // Pour l'instant, juste recharger les données
    await loadPartnership();

  } catch (err) {
    console.error('Failed to save partnership:', err);
    error.value = 'Impossible de sauvegarder les modifications';
  } finally {
    saving.value = false;
  }
}

function onCancel() {
  router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors`);
}

/**
 * Gère la validation d'un partenariat avec confirmation utilisateur
 */
async function handleValidatePartnership() {
  if (!partnership.value) return;

  // Demander confirmation avec modale personnalisée
  const confirmed = await confirm({
    ...PARTNERSHIP_CONFIRM.VALIDATE,
    message: PARTNERSHIP_CONFIRM.VALIDATE.message(partnership.value.company_name)
  });

  if (!confirmed) return;

  isValidating.value = true;
  error.value = null;

  const result = await validatePartnership(
    partnership.value,
    orgSlug.value,
    eventSlug.value,
    sponsorId.value,
    loadPartnership
  );

  if (!result.success && result.error) {
    error.value = result.error;
  }

  isValidating.value = false;
}

/**
 * Gère le refus d'un partenariat avec confirmation utilisateur
 */
async function handleDeclinePartnership() {
  if (!partnership.value) return;

  // Demander confirmation avec modale personnalisée
  const confirmed = await confirm({
    ...PARTNERSHIP_CONFIRM.DECLINE,
    message: PARTNERSHIP_CONFIRM.DECLINE.message(partnership.value.company_name)
  });

  if (!confirmed) return;

  isDeclining.value = true;
  error.value = null;

  const result = await declinePartnership(
    partnership.value,
    orgSlug.value,
    eventSlug.value,
    sponsorId.value,
    () => router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/sponsors`)
  );

  if (!result.success && result.error) {
    error.value = result.error;
  }

  isDeclining.value = false;
}

/**
 * Gestionnaire de confirmation de la modale
 */
function handleConfirm() {
  confirmModalConfirm();
}

/**
 * Gestionnaire d'annulation de la modale
 */
function handleCancel() {
  confirmModalCancel();
}

onMounted(() => {
  loadPartnership();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
