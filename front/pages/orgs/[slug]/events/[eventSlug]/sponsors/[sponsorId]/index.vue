<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
          <p class="text-sm text-gray-600 mt-1">Partenariat</p>
        </div>
        <div v-if="partnership && !partnership.validated_pack_id" class="flex gap-3">
          <UButton
            color="neutral"
            variant="outline"
            :loading="isSuggesting"
            :disabled="isValidating || isDeclining"
            :aria-label="`Proposer un autre pack à ${partnership.company_name}`"
            @click="handleSuggestPack"
          >
            <i class="i-heroicons-arrow-path mr-2" aria-hidden="true" />
            Proposer un autre pack
          </UButton>
          <UButton
            color="error"
            variant="outline"
            :loading="isDeclining"
            :disabled="isValidating || isSuggesting"
            :aria-label="`Refuser le partenariat avec ${partnership.company_name}`"
            @click="handleDeclinePartnership"
          >
            <i class="i-heroicons-x-mark mr-2" aria-hidden="true" />
            Refuser
          </UButton>
          <UButton
            color="primary"
            :loading="isValidating"
            :disabled="isDeclining || isSuggesting"
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

    <!-- Modale de suggestion de pack -->
    <SuggestPackModal
      v-model="isSuggestModalOpen"
      :event-slug="eventSlug"
      :current-pack-id="partnership?.selected_pack_id || undefined"
      :current-language="partnership?.language"
      @submit="handleSuggestPackSubmit"
    />
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, postOrgsEventsPartnershipSuggestion } from "~/utils/api";
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
const isSuggesting = ref(false);

// État pour la modale de suggestion
const isSuggestModalOpen = ref(false);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    // Charger directement le partenariat par son ID
    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    const { partnership: p, company, event } = response.data;

    // Extraire les options du pack sélectionné avec leurs informations complètes
    // Note: L'API retourne "options" alors que le schéma TypeScript définit "optional_options"
    const packOptions = ((p.selected_pack as any)?.options || p.selected_pack?.optional_options || []).map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null
    }));
    const optionIds = packOptions.map(opt => opt.id);

    // Mapper les données de DetailedPartnershipResponseSchema vers ExtendedPartnershipItem
    partnership.value = {
      id: p.id,
      contact: {
        display_name: p.contact_name,
        role: p.contact_role
      },
      company_name: company.name,
      event_name: event.name,
      selected_pack_id: p.selected_pack?.id || null,
      selected_pack_name: p.selected_pack?.name || null,
      suggested_pack_id: p.suggestion_pack?.id || null,
      suggested_pack_name: p.suggestion_pack?.name || null,
      validated_pack_id: p.validated_pack?.id || null,
      language: p.language,
      phone: p.phone || null,
      emails: p.emails.join(', '),
      created_at: p.created_at,
      // Champs étendus depuis ExtendedPartnershipItem
      validated: p.process_status?.validated_at !== null && p.process_status?.validated_at !== undefined,
      paid: p.process_status?.billing_status === 'paid',
      suggestion: false, // suggested_at n'existe pas dans PartnershipProcessStatusSchema
      agreement_generated: p.process_status?.agreement_url !== null && p.process_status?.agreement_url !== undefined,
      agreement_signed: p.process_status?.agreement_signed_url !== null && p.process_status?.agreement_signed_url !== undefined,
      option_ids: optionIds,
      pack_options: packOptions
    };
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

/**
 * Ouvre la modale pour proposer un autre pack
 */
function handleSuggestPack() {
  isSuggestModalOpen.value = true;
}

/**
 * Gère la soumission de la suggestion de pack
 */
async function handleSuggestPackSubmit(data: { packId: string; language: string; optionIds: string[] }) {
  if (!partnership.value) return;

  try {
    isSuggesting.value = true;
    error.value = null;

    // Créer les option_selections à partir des optionIds
    // Par défaut, on utilise quantitative_selection avec une quantité de 1
    const option_selections = data.optionIds.map(optionId => ({
      type: 'quantitative_selection' as const,
      option_id: optionId,
      selected_quantity: 1
    }));

    await postOrgsEventsPartnershipSuggestion(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value,
      {
        pack_id: data.packId,
        language: data.language,
        option_selections
      }
    );

    // Fermer la modale
    isSuggestModalOpen.value = false;

    // Recharger les données du partenariat
    await loadPartnership();

    // TODO: Afficher un message de succès à l'utilisateur
    console.log('Suggestion de pack envoyée avec succès');

  } catch (err) {
    console.error('Failed to suggest pack:', err);
    error.value = 'Impossible d\'envoyer la suggestion de pack';
  } finally {
    isSuggesting.value = false;
  }
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
