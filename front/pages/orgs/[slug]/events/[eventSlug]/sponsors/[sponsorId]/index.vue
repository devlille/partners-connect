<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
          <p class="text-sm text-gray-600 mt-1">Partenariat</p>
        </div>
        <div class="flex gap-3">
          <UButton
            color="neutral"
            variant="outline"
            :to="`/${eventSlug}/${sponsorId}`"
            target="_blank"
            :aria-label="`Voir la page publique du partenariat avec ${partnership?.company_name || 'ce sponsor'}`"
          >
            <i class="i-heroicons-arrow-top-right-on-square mr-2" aria-hidden="true" />
            Page publique
          </UButton>
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
          <UButton
            v-if="partnership && !partnership.paid"
            color="success"
            variant="outline"
            :loading="isMarkingPaid"
            :aria-label="`Marquer comme payé le partenariat avec ${partnership.company_name}`"
            @click="handleMarkAsPaid"
          >
            <i class="i-heroicons-currency-dollar mr-2" aria-hidden="true" />
            Marquer comme payé
          </UButton>
          <div
            v-else-if="partnership && partnership.paid"
            class="flex items-center gap-2 px-3 py-2 bg-green-50 text-green-700 rounded-md border border-green-200"
          >
            <i class="i-heroicons-check-circle" aria-hidden="true" />
            <span class="text-sm font-medium">Payé</span>
          </div>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="space-y-6">
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations du partenariat</h2>
          <PartnershipForm
            :partnership="partnership"
            :loading="saving"
            @save="onSave"
            @cancel="onCancel"
          />
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de facturation</h2>
          <BillingForm
            :event-slug="eventSlug"
            :partnership-id="sponsorId"
            @saved="handleBillingSaved"
          />
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <h2 id="organiser-section" class="text-lg font-semibold text-gray-900 mb-4">Organisateur assigné</h2>
          <div v-if="partnership?.organiser" class="flex items-center justify-between p-4 bg-gray-50 rounded-lg" role="region" aria-labelledby="organiser-section">
            <div class="flex items-center gap-3">
              <div v-if="partnership.organiser.picture_url" class="w-10 h-10 rounded-full overflow-hidden">
                <img :src="partnership.organiser.picture_url" :alt="`Photo de profil de ${partnership.organiser.display_name || partnership.organiser.email}`" class="w-full h-full object-cover">
              </div>
              <div v-else class="w-10 h-10 rounded-full bg-primary-500 text-white flex items-center justify-center" role="img" :aria-label="`Initiales de ${partnership.organiser.display_name || partnership.organiser.email}`">
                <span class="text-sm font-semibold" aria-hidden="true">{{ getInitials(partnership.organiser.display_name || partnership.organiser.email) }}</span>
              </div>
              <div>
                <div class="text-sm font-medium text-gray-900">{{ partnership.organiser.display_name || partnership.organiser.email }}</div>
                <div class="text-xs text-gray-500">{{ partnership.organiser.email }}</div>
              </div>
            </div>
            <UButton
              color="error"
              variant="outline"
              size="sm"
              :loading="isUnassigning"
              :aria-label="`Retirer ${partnership.organiser.display_name || partnership.organiser.email} comme organisateur`"
              @click="handleUnassignOrganiser"
            >
              <i class="i-heroicons-x-mark mr-1" aria-hidden="true" />
              Retirer
            </UButton>
          </div>
          <div v-else class="p-4 bg-gray-50 rounded-lg" role="region" aria-labelledby="organiser-section">
            <p class="text-sm text-gray-600 mb-3">Aucun organisateur assigné</p>
            <div v-if="loadingUsers" class="text-sm text-gray-500" role="status" aria-live="polite">
              <i class="i-heroicons-arrow-path animate-spin mr-2" aria-hidden="true" />
              Chargement des utilisateurs...
            </div>
            <div v-else class="flex gap-3">
              <label for="organiser-select" class="sr-only">Sélectionner un membre de l'équipe pour gérer ce partenariat</label>
              <select
                id="organiser-select"
                v-model="selectedUserEmail"
                class="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-primary-500 focus:border-primary-500"
                aria-describedby="organiser-help"
                :aria-invalid="!!assignError"
              >
                <option value="">Sélectionner un organisateur</option>
                <option v-for="user in sortedOrgUsers" :key="user.email" :value="user.email">
                  {{ user.display_name || user.email }}
                </option>
              </select>
              <span id="organiser-help" class="sr-only">
                Choisissez un membre de l'équipe pour gérer ce partenariat. Seuls les membres de votre organisation peuvent être assignés.
              </span>
              <UButton
                color="primary"
                :loading="isAssigning"
                :disabled="!selectedUserEmail || loadingUsers"
                :aria-label="selectedUserEmail ? `Assigner ${selectedUserEmail} comme organisateur` : 'Veuillez sélectionner un utilisateur pour l\'assigner'"
                @click="handleAssignOrganiser"
              >
                <i class="i-heroicons-user-plus mr-1" aria-hidden="true" />
                Assigner
              </UButton>
            </div>
            <p v-if="assignError" id="organiser-error" class="text-sm text-red-600 mt-2" role="alert" aria-live="assertive">
              {{ assignError }}
            </p>
          </div>
        </div>
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
import { getEventsPartnershipDetailed, updatePartnershipContactInfo, postOrgsEventsPartnershipSuggestion, postOrgsEventsPartnershipBilling, postPartnershipOrganiser, deletePartnershipOrganiser, getOrgsUsers, type UserSchema } from "~/utils/api";
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
const isMarkingPaid = ref(false);

// État pour la modale de suggestion
const isSuggestModalOpen = ref(false);

// États pour l'assignment d'organisateur
const selectedUserEmail = ref('');
const orgUsers = ref<UserSchema[]>([]);
const loadingUsers = ref(false);
const isAssigning = ref(false);
const isUnassigning = ref(false);
const assignError = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

/**
 * Utilisateurs triés par ordre alphabétique (display_name ou email)
 */
const sortedOrgUsers = computed(() => {
  return [...orgUsers.value].sort((a, b) => {
    const nameA = a.display_name || a.email;
    const nameB = b.display_name || b.email;
    return nameA.localeCompare(nameB);
  });
});

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    // Charger directement le partenariat par son ID
    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    const { partnership: p, company: c, event } = response.data;

    // Extraire les options du pack sélectionné avec leurs informations complètes
    // Note: L'API retourne "options" alors que le schéma TypeScript définit "optional_options"
    const packOptions = ((p.selected_pack as any)?.options || p.selected_pack?.optional_options || []).map((opt: any) => ({
      id: opt.id,
      name: opt.name,
      description: opt.description || null,
      type: opt.type,
      price: opt.price ?? null,
      quantity: opt.quantity ?? null,
      total_price: opt.total_price ?? null,
      selected_value: opt.selected_value ?? null
    }));
    const optionIds = packOptions.map((opt: { id: string }) => opt.id);

    // Mapper les données de DetailedPartnershipResponseSchema vers ExtendedPartnershipItem
    partnership.value = {
      id: p.id,
      contact: {
        display_name: p.contact_name,
        role: p.contact_role
      },
      company_name: c.name,
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
      organiser: p.organiser || null,
      // Champs étendus depuis ExtendedPartnershipItem
      validated: p.process_status?.validated_at !== null && p.process_status?.validated_at !== undefined,
      paid: p.process_status?.billing_status?.toLowerCase() === 'paid',
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

    // Appeler l'API de mise à jour
    await updatePartnershipContactInfo(
      eventSlug.value,
      sponsorId.value,
      {
        contact_name: data.contact_name,
        contact_role: data.contact_role,
        language: data.language,
        emails: data.emails,
        phone: data.phone
      }
    );

    // Recharger les données pour afficher les modifications
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
 * Gère le marquage du partenariat comme payé
 */
async function handleMarkAsPaid() {
  if (!partnership.value) return;

  isMarkingPaid.value = true;
  error.value = null;

  try {
    await postOrgsEventsPartnershipBilling(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value,
      'paid'
    );

    // Recharger les données pour mettre à jour l'affichage
    await loadPartnership();
  } catch (err) {
    console.error('Failed to mark partnership as paid:', err);
    error.value = 'Impossible de marquer le partenariat comme payé';
  } finally {
    isMarkingPaid.value = false;
  }
}

/**
 * Gère la sauvegarde des informations de facturation
 */
function handleBillingSaved() {
  // Recharger les données du partenariat pour mettre à jour le statut
  loadPartnership();
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

/**
 * Obtenir les initiales d'un nom
 */
function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return parts[0].substring(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Charger les utilisateurs de l'organisation
 */
async function loadOrgUsers() {
  try {
    loadingUsers.value = true;
    const response = await getOrgsUsers(orgSlug.value);
    orgUsers.value = response.data;
  } catch (err) {
    console.error('Failed to load org users:', err);
    assignError.value = 'Impossible de charger la liste des utilisateurs';
  } finally {
    loadingUsers.value = false;
  }
}

/**
 * Assigner un organisateur au partenariat
 */
async function handleAssignOrganiser() {
  if (!selectedUserEmail.value) return;

  try {
    isAssigning.value = true;
    assignError.value = null;

    await postPartnershipOrganiser(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value,
      { email: selectedUserEmail.value }
    );

    // Recharger les données du partenariat
    await loadPartnership();

    // Réinitialiser le champ
    selectedUserEmail.value = '';
  } catch (err) {
    console.error('Failed to assign organiser:', err);
    assignError.value = 'Impossible d\'assigner l\'organisateur. Vérifiez que l\'email est correct et que l\'utilisateur est membre de l\'organisation.';
  } finally {
    isAssigning.value = false;
  }
}

/**
 * Retirer l'organisateur assigné
 */
async function handleUnassignOrganiser() {
  try {
    isUnassigning.value = true;
    assignError.value = null;

    await deletePartnershipOrganiser(
      orgSlug.value,
      eventSlug.value,
      sponsorId.value
    );

    // Recharger les données du partenariat
    await loadPartnership();
  } catch (err) {
    console.error('Failed to unassign organiser:', err);
    error.value = 'Impossible de retirer l\'organisateur';
  } finally {
    isUnassigning.value = false;
  }
}

onMounted(() => {
  loadPartnership();
  loadOrgUsers();
});

// Recharger si les slugs changent
watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
