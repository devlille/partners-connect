<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <!-- Header -->
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <PageTitle>{{ partnership?.company_name || 'Partenariat' }}</PageTitle>
              <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
                {{ partnership?.event_name }}
              </p>
            </div>
          </div>

          <!-- Welcome message for non-validated partnerships -->
          <div v-if="!loading && !error && partnership && !partnership.validated" class="mt-4 pt-4 border-t border-gray-200">
            <p class="text-sm text-gray-700 leading-relaxed">
              Voici la page dédiée à votre partenariat avec <strong>{{ partnership.event_name }}</strong>.
              Nous vous recommandons de l'ajouter à vos favoris pour y accéder facilement.
              Le lien vers cette page vous sera également communiqué dans nos prochains échanges par email.
            </p>
            <p class="text-sm text-gray-700 leading-relaxed mt-3">
              Votre demande de partenariat a bien été enregistrée et est actuellement en cours d'examen par l'équipe organisatrice.
              Nous reviendrons vers vous très prochainement.
            </p>
          </div>

          <!-- Information message for validated but unpaid partnerships -->
          <div v-if="!loading && !error && partnership && partnership.validated && !partnership.paid" class="mt-4 pt-4 border-t border-gray-200">
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div class="flex items-start gap-3">
                <i class="i-heroicons-information-circle text-blue-600 text-xl shrink-0 mt-0.5" aria-hidden="true" />
                <div>
                  <h3 class="text-sm font-semibold text-blue-900 mb-2">
                    Complétez vos informations
                  </h3>
                  <p class="text-sm text-blue-700 mb-3">
                    Votre partenariat a été validé ! Pour finaliser le processus, merci de compléter les informations manquantes dans les onglets suivants :
                  </p>
                  <ul class="text-sm text-blue-700 space-y-1 ml-5 list-disc">
                    <li>
                      <NuxtLink :to="`/${eventSlug}/${partnershipId}`" class="font-medium hover:underline">
                        Partenariat
                      </NuxtLink>
                      - Vérifiez et complétez vos coordonnées de contact
                    </li>
                    <li>
                      <NuxtLink :to="`/${eventSlug}/${partnershipId}/company`" class="font-medium hover:underline">
                        Entreprise
                      </NuxtLink>
                      - Renseignez les informations de votre société
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </header>

        <!-- Loading State -->
        <div v-if="loading" role="status" aria-live="polite" aria-label="Chargement des données">
          <TableSkeleton :columns="4" :rows="6" />
          <span class="sr-only">Chargement des informations du partenariat...</span>
        </div>

        <!-- Error State -->
        <AlertMessage v-else-if="error" type="error" :message="error" />

        <!-- Suggested Pack Section -->
        <section
          v-if="!loading && !error && partnership && partnership.suggested_pack_id && !partnership.validated_pack_id"
          class="bg-amber-50 border border-amber-200 rounded-lg p-6 mb-6"
          aria-labelledby="suggestion-heading"
        >
          <div class="flex items-start gap-3">
            <i class="i-heroicons-light-bulb text-amber-600 text-xl flex-shrink-0 mt-0.5" aria-hidden="true" />
            <div class="flex-1">
              <h3 id="suggestion-heading" class="text-sm font-semibold text-amber-900 mb-2">
                Nouveau pack suggéré
              </h3>
              <p class="text-sm text-amber-700 mb-4">
                L'équipe organisatrice vous propose le pack <strong>{{ partnership.suggested_pack_name }}</strong> en remplacement du pack <strong>{{ partnership.selected_pack_name }}</strong> que vous aviez sélectionné.
              </p>
              <div class="flex gap-3">
                <UButton
                  color="primary"
                  size="sm"
                  :loading="acceptingSuggestion"
                  :disabled="rejectingSuggestion"
                  @click="handleAcceptSuggestion"
                >
                  <i class="i-heroicons-check mr-1" aria-hidden="true" />
                  Accepter cette suggestion
                </UButton>
                <UButton
                  color="neutral"
                  variant="outline"
                  size="sm"
                  :loading="rejectingSuggestion"
                  :disabled="acceptingSuggestion"
                  @click="handleRejectSuggestion"
                >
                  <i class="i-heroicons-x-mark mr-1" aria-hidden="true" />
                  Refuser
                </UButton>
              </div>
            </div>
          </div>
        </section>

        <!-- Partnership Information -->
        <section
          v-if="!loading && !error && partnership"
          class="bg-white rounded-lg shadow p-6"
          aria-labelledby="partnership-heading"
          :aria-busy="savingPartnership ? 'true' : 'false'"
        >
          <h2 id="partnership-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Informations du partenariat
          </h2>
          <div v-if="savingPartnership" role="status" aria-live="polite" class="sr-only">
            Enregistrement des informations du partenariat en cours...
          </div>
          <PartnershipForm
            :partnership="partnership"
            :loading="savingPartnership"
            :show-admin-actions="false"
            :readonly="false"
            @save="handlePartnershipSave"
          />
        </section>

        <!-- Billing Information -->
        <section
          v-if="!loading && !error && partnership && partnership.validated_pack_id"
          class="bg-white rounded-lg shadow p-6 mt-6"
          aria-labelledby="billing-heading"
          :aria-busy="savingBilling ? 'true' : 'false'"
        >
          <h2 id="billing-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Informations de facturation
          </h2>
          <div v-if="savingBilling" role="status" aria-live="polite" class="sr-only">
            Enregistrement des informations de facturation en cours...
          </div>
          <BillingForm
            :partnership="partnership"
            :billing="billing"
            :readonly="false"
            :loading="savingBilling"
            @save="handleBillingSave"
          />
        </section>

        <!-- Documents Section -->
        <section
          v-if="!loading && !error && partnership && partnership.validated_pack_id"
          class="bg-white rounded-lg shadow p-6 mt-6"
          aria-labelledby="documents-heading"
        >
          <h2 id="documents-heading" class="text-lg font-semibold text-gray-900 mb-4">
            Documents
          </h2>

          <!-- Message if no documents available -->
          <div
            v-if="!partnership.quote_url && !partnership.invoice_url && !partnership.agreement_url && !partnership.agreement_signed_url"
            class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg"
          >
            <i class="i-heroicons-information-circle text-gray-400 text-xl shrink-0 mt-0.5" aria-hidden="true" />
            <p class="text-sm text-gray-600">
              Aucun document n'est disponible pour le moment. Les documents seront générés par l'équipe organisatrice.
            </p>
          </div>

          <div v-else class="space-y-3">
            <!-- Quote Document -->
            <div
              v-if="partnership.quote_url"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <i class="i-heroicons-document-text text-gray-400 text-xl" aria-hidden="true" />
                <div>
                  <p class="text-sm font-medium text-gray-900">Devis</p>
                  <p class="text-xs text-gray-500">Document de proposition commerciale</p>
                </div>
              </div>
              <UButton
                :to="partnership.quote_url"
                target="_blank"
                color="neutral"
                variant="outline"
                size="sm"
                :aria-label="`Télécharger le devis`"
              >
                <i class="i-heroicons-arrow-down-tray mr-1" aria-hidden="true" />
                Télécharger
              </UButton>
            </div>

            <!-- Invoice Document -->
            <div
              v-if="partnership.invoice_url"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <i class="i-heroicons-document-text text-gray-400 text-xl" aria-hidden="true" />
                <div>
                  <p class="text-sm font-medium text-gray-900">Facture</p>
                  <p class="text-xs text-gray-500">Document de facturation</p>
                </div>
              </div>
              <UButton
                :to="partnership.invoice_url"
                target="_blank"
                color="neutral"
                variant="outline"
                size="sm"
                :aria-label="`Télécharger la facture`"
              >
                <i class="i-heroicons-arrow-down-tray mr-1" aria-hidden="true" />
                Télécharger
              </UButton>
            </div>

            <!-- Agreement Document -->
            <div
              v-if="partnership.agreement_url || partnership.agreement_signed_url"
              class="space-y-3"
            >
              <div class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
                <div class="flex items-center gap-3">
                  <i class="i-heroicons-document-check text-gray-400 text-xl" aria-hidden="true" />
                  <div>
                    <p class="text-sm font-medium text-gray-900">Convention de partenariat</p>
                    <p class="text-xs text-gray-500">Document contractuel</p>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <UButton
                    v-if="partnership.agreement_signed_url"
                    :to="partnership.agreement_signed_url"
                    target="_blank"
                    color="neutral"
                    variant="outline"
                    size="sm"
                    :aria-label="`Télécharger la convention signée`"
                  >
                    <i class="i-heroicons-arrow-down-tray mr-1" aria-hidden="true" />
                    Télécharger (signée)
                  </UButton>
                  <UButton
                    v-else-if="partnership.agreement_url"
                    :to="partnership.agreement_url"
                    target="_blank"
                    color="neutral"
                    variant="outline"
                    size="sm"
                    :aria-label="`Télécharger la convention`"
                  >
                    <i class="i-heroicons-arrow-down-tray mr-1" aria-hidden="true" />
                    Télécharger
                  </UButton>
                  <span
                    v-if="partnership.agreement_signed_url"
                    class="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-green-700 bg-green-50 rounded-full"
                  >
                    <i class="i-heroicons-check-circle" aria-hidden="true" />
                    Signée
                  </span>
                </div>
              </div>

              <!-- Upload Signed Agreement -->
              <div
                v-if="partnership.agreement_url && !partnership.agreement_signed_url"
                class="p-4 border border-blue-200 bg-blue-50 rounded-lg"
              >
                <div class="flex items-start gap-3">
                  <i class="i-heroicons-arrow-up-tray text-blue-600 text-xl flex-shrink-0 mt-0.5" aria-hidden="true" />
                  <div class="flex-1">
                    <h4 class="text-sm font-semibold text-blue-900 mb-1">
                      Uploader la convention signée
                    </h4>
                    <p class="text-xs text-blue-700 mb-3">
                      Une fois la convention signée, veuillez uploader le document signé au format PDF.
                    </p>

                    <AlertMessage
                      v-if="uploadError"
                      type="error"
                      :message="uploadError"
                      class="mb-3"
                    />

                    <div class="flex items-center gap-2">
                      <UButton
                        color="primary"
                        size="sm"
                        icon="i-heroicons-arrow-up-tray"
                        :loading="uploadingAgreement"
                        :disabled="uploadingAgreement"
                        @click="triggerAgreementFileInput"
                      >
                        Sélectionner le fichier PDF
                      </UButton>
                      <span v-if="uploadingAgreement" class="text-xs text-gray-600">
                        Upload en cours...
                      </span>
                    </div>

                    <input
                      ref="agreementFileInput"
                      type="file"
                      accept=".pdf,application/pdf"
                      class="hidden"
                      @change="handleAgreementFileSelect"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- RIB Document -->
            <div
              v-if="organisation && (organisation as any).rib_url"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <i class="i-heroicons-banknotes text-gray-400 text-xl" aria-hidden="true" />
                <div>
                  <p class="text-sm font-medium text-gray-900">RIB (Relevé d'Identité Bancaire)</p>
                  <p class="text-xs text-gray-500">Coordonnées bancaires de l'organisation</p>
                </div>
              </div>
              <UButton
                :to="(organisation as any).rib_url"
                target="_blank"
                color="neutral"
                variant="outline"
                size="sm"
                :aria-label="`Télécharger le RIB`"
              >
                <i class="i-heroicons-arrow-down-tray mr-1" aria-hidden="true" />
                Télécharger
              </UButton>
            </div>
          </div>
        </section>

        <!-- Message if not validated -->
        <section
          v-if="!loading && !error && partnership && !partnership.validated_pack_id"
          class="bg-blue-50 border border-blue-200 rounded-lg p-6 mt-6"
        >
          <div class="flex items-start gap-3">
            <i class="i-heroicons-information-circle text-blue-600 text-xl flex-shrink-0 mt-0.5" aria-hidden="true" />
            <div>
              <h3 class="text-sm font-semibold text-blue-900 mb-1">
                Partenariat en attente de validation
              </h3>
              <p class="text-sm text-blue-700">
                Votre demande de partenariat est en cours de traitement. Les informations de facturation seront disponibles une fois que votre partenariat aura été validé par nos équipes.
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import PartnershipForm from "~/components/partnership/PartnershipForm.vue";
import BillingForm from "~/components/partnership/BillingForm.vue";
import { updatePartnershipContactInfo, postEventsPartnershipSuggestionApprove, postEventsPartnershipSuggestionDecline, postEventsPartnershipSignedAgreement } from "~/utils/api";

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    // Validate both eventSlug and partnershipId format (alphanumeric, hyphens, underscores)
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;

    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  }
});

const toast = useToast();

const {
  eventSlug,
  partnershipId,
  partnership,
  company,
  billing,
  organisation,
  loading,
  error,
  savingBilling,
  loadPartnership,
  handleBillingSave
} = usePublicPartnership();

const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

// États pour les actions de suggestion
const acceptingSuggestion = ref(false);
const rejectingSuggestion = ref(false);

// État pour la sauvegarde du partenariat
const savingPartnership = ref(false);

// États pour l'upload de la convention signée
const uploadingAgreement = ref(false);
const uploadError = ref<string | null>(null);
const agreementFileInput = ref<HTMLInputElement>();

/**
 * Handle partnership information save
 */
async function handlePartnershipSave(data: any) {
  try {
    savingPartnership.value = true;
    error.value = null;

    // Appeler l'API de mise à jour
    await updatePartnershipContactInfo(
      eventSlug.value,
      partnershipId.value,
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

    toast.add({
      title: 'Succès',
      description: 'Les informations du partenariat ont été mises à jour',
      color: 'success'
    });
  } catch (err: any) {
    console.error('Failed to update partnership:', err);
    const errorMessage = `Impossible de mettre à jour les informations du partenariat: ${err.message || 'Erreur inconnue'}`;
    error.value = errorMessage;

    toast.add({
      title: 'Erreur',
      description: errorMessage,
      color: 'error'
    });
  } finally {
    savingPartnership.value = false;
  }
}

/**
 * Handle accepting the suggested pack
 */
async function handleAcceptSuggestion() {
  try {
    acceptingSuggestion.value = true;
    error.value = null;

    await postEventsPartnershipSuggestionApprove(
      eventSlug.value,
      partnershipId.value
    );

    // Recharger les données du partenariat
    await loadPartnership();

    toast.add({
      title: 'Suggestion acceptée',
      description: 'Votre pack a été mis à jour avec succès',
      color: 'success',
      timeout: 3000
    });
  } catch (err: any) {
    console.error('Failed to accept suggestion:', err);
    const errorMessage = `Impossible d'accepter la suggestion: ${err.message || 'Erreur inconnue'}`;
    error.value = errorMessage;

    toast.add({
      title: 'Erreur',
      description: errorMessage,
      color: 'error',
      timeout: 5000
    });
  } finally {
    acceptingSuggestion.value = false;
  }
}

/**
 * Handle rejecting the suggested pack
 */
async function handleRejectSuggestion() {
  try {
    rejectingSuggestion.value = true;
    error.value = null;

    await postEventsPartnershipSuggestionDecline(
      eventSlug.value,
      partnershipId.value
    );

    // Recharger les données du partenariat
    await loadPartnership();

    toast.add({
      title: 'Suggestion refusée',
      description: 'Vous conservez votre pack initial',
      color: 'success',
      timeout: 3000
    });
  } catch (err: any) {
    console.error('Failed to reject suggestion:', err);
    const errorMessage = `Impossible de refuser la suggestion: ${err.message || 'Erreur inconnue'}`;
    error.value = errorMessage;

    toast.add({
      title: 'Erreur',
      description: errorMessage,
      color: 'error',
      timeout: 5000
    });
  } finally {
    rejectingSuggestion.value = false;
  }
}

/**
 * Trigger file input for signed agreement
 */
function triggerAgreementFileInput() {
  uploadError.value = null;
  agreementFileInput.value?.click();
}

/**
 * Handle signed agreement file selection
 */
async function handleAgreementFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  
  if (!file) return;

  // Vérifier que c'est un PDF
  if (file.type !== 'application/pdf') {
    uploadError.value = 'Veuillez sélectionner un fichier PDF';
    toast.add({
      title: 'Format invalide',
      description: 'Seuls les fichiers PDF sont acceptés',
      color: 'error',
      timeout: 5000
    });
    return;
  }

  // Vérifier la taille (10MB max)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    uploadError.value = 'Le fichier ne doit pas dépasser 10MB';
    toast.add({
      title: 'Fichier trop volumineux',
      description: 'Le fichier PDF ne doit pas dépasser 10MB',
      color: 'error',
      timeout: 5000
    });
    return;
  }

  await handleAgreementUpload(file);
}

/**
 * Upload signed agreement
 */
async function handleAgreementUpload(file: File) {
  try {
    uploadingAgreement.value = true;
    uploadError.value = null;

    await postEventsPartnershipSignedAgreement(
      eventSlug.value,
      partnershipId.value,
      { file }
    );

    toast.add({
      title: 'Succès',
      description: 'La convention signée a été uploadée avec succès',
      color: 'success',
      timeout: 3000
    });

    // Réinitialiser l'input
    if (agreementFileInput.value) {
      agreementFileInput.value.value = '';
    }

    // Recharger les données du partenariat
    await loadPartnership();
  } catch (err: any) {
    console.error('Failed to upload signed agreement:', err);
    const errorMessage = err.response?.data?.message || 'Impossible d\'uploader la convention signée';
    uploadError.value = errorMessage;

    toast.add({
      title: 'Erreur',
      description: errorMessage,
      color: 'error',
      timeout: 5000
    });
  } finally {
    uploadingAgreement.value = false;
  }
}

// Sidebar navigation configuration
const sidebarLinks = computed(() => {
  const partnershipComplete = isPartnershipComplete(partnership.value);
  const companyComplete = isCompanyComplete(company.value);

  return [
    {
      label: 'Partenariat',
      icon: 'i-heroicons-hand-raised',
      to: `/${eventSlug.value}/${partnershipId.value}`,
      badge: !partnershipComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
    {
      label: 'Entreprise',
      icon: 'i-heroicons-building-office',
      to: `/${eventSlug.value}/${partnershipId.value}/company`,
      badge: !companyComplete ? {
        label: '!',
        color: 'error' as const,
        title: 'Informations incomplètes'
      } : undefined
    },
    {
      label: 'Offres d\'emploi',
      icon: 'i-heroicons-briefcase',
      to: `/${eventSlug.value}/${partnershipId.value}/job-offers`
    },
    {
      label: 'Liens utiles',
      icon: 'i-heroicons-link',
      to: `/${eventSlug.value}/${partnershipId.value}/external-links`
    },
    {
      label: 'Prestataires',
      icon: 'i-heroicons-user-group',
      to: `/${eventSlug.value}/${partnershipId.value}/providers`
    }
  ];
});

onMounted(() => {
  loadPartnership();
});

// Reload if partnership ID or event slug changes
watch([eventSlug, partnershipId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `${partnership.value?.company_name || 'Partenariat'} | DevLille`)
});
</script>
