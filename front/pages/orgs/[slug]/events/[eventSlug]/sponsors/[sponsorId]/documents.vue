<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6">
      <div>
        <PageTitle>{{ partnership?.company_name || 'Sponsor' }}</PageTitle>
        <p class="text-sm text-gray-600 mt-1">Documents</p>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <div v-else class="bg-white rounded-lg shadow p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Documents</h2>

        <div class="space-y-6">
          <!-- Logo de l'entreprise -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-photo text-3xl text-primary-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Logo de l'entreprise</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Téléchargez le logo de l'entreprise (PNG, JPG, max 5MB).
                </p>

                <div v-if="logoError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ logoError }}
                </div>

                <div v-if="logoSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ logoSuccess }}
                </div>

                <div v-if="companyLogo" class="mb-4">
                  <div class="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded">
                    <img :src="companyLogo" alt="Logo de l'entreprise" class="h-12 w-12 object-contain" />
                    <div class="flex-1">
                      <p class="text-sm font-medium text-gray-900">Logo actuel</p>
                      <a
                        :href="companyLogo"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="text-sm text-primary-600 hover:text-primary-800"
                      >
                        Voir en grand
                      </a>
                    </div>
                  </div>
                </div>

                <div class="flex gap-3 items-center">
                  <input
                    ref="logoFileInput"
                    type="file"
                    accept="image/png,image/jpeg,image/jpg"
                    class="hidden"
                    @change="handleLogoFileChange"
                  />
                  <UButton
                    color="primary"
                    variant="outline"
                    :loading="isUploadingLogo"
                    @click="() => logoFileInput?.click()"
                  >
                    <i class="i-heroicons-arrow-up-tray mr-2" aria-hidden="true" />
                    {{ companyLogo ? 'Remplacer le logo' : 'Télécharger le logo' }}
                  </UButton>

                  <span v-if="selectedLogoFile" class="text-sm text-gray-600">
                    {{ selectedLogoFile.name }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Devis -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-document-text text-3xl text-primary-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Devis</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Générez le devis au format PDF pour ce sponsor.
                </p>

                <div v-if="quoteError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ quoteError }}
                </div>

                <div class="flex gap-3 items-center">
                  <UButton
                    color="primary"
                    :loading="isGeneratingQuote"
                    :aria-label="`Générer le devis pour ${partnership?.company_name}`"
                    @click="handleGenerateQuote"
                  >
                    <i class="i-heroicons-document-arrow-down mr-2" aria-hidden="true" />
                    Générer le devis
                  </UButton>

                  <a
                    v-if="quoteUrl"
                    :href="quoteUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 text-primary-600 hover:text-primary-800 font-medium text-sm"
                  >
                    Voir le dernier devis généré
                    <i class="i-heroicons-arrow-top-right-on-square" aria-hidden="true" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Facture -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-document-text text-3xl text-primary-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Facture</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Générez la facture au format PDF pour ce sponsor.
                </p>

                <div v-if="invoiceError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ invoiceError }}
                </div>

                <div class="flex gap-3 items-center">
                  <UButton
                    color="primary"
                    :loading="isGeneratingInvoice"
                    :aria-label="`Générer la facture pour ${partnership?.company_name}`"
                    @click="handleGenerateInvoice"
                  >
                    <i class="i-heroicons-document-arrow-down mr-2" aria-hidden="true" />
                    Générer la facture
                  </UButton>

                  <a
                    v-if="invoiceUrl"
                    :href="invoiceUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 text-primary-600 hover:text-primary-800 font-medium text-sm"
                  >
                    Voir la dernière facture générée
                    <i class="i-heroicons-arrow-top-right-on-square" aria-hidden="true" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Convention de partenariat -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-document-check text-3xl text-primary-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Convention de partenariat</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Générez la convention de partenariat au format PDF pour ce sponsor.
                </p>

                <div v-if="agreementError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ agreementError }}
                </div>

                <div class="flex gap-3 items-center">
                  <UButton
                    color="primary"
                    :loading="isGeneratingAgreement"
                    :aria-label="`Générer la convention pour ${partnership?.company_name}`"
                    @click="handleGenerateAgreement"
                  >
                    <i class="i-heroicons-document-arrow-down mr-2" aria-hidden="true" />
                    Générer la convention
                  </UButton>

                  <a
                    v-if="agreementUrl"
                    :href="agreementUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 text-primary-600 hover:text-primary-800 font-medium text-sm"
                  >
                    Voir la dernière convention générée
                    <i class="i-heroicons-arrow-top-right-on-square" aria-hidden="true" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          <!-- Convention signée -->
          <div class="bg-gray-50 rounded-lg p-6">
            <div class="flex items-start gap-4">
              <div class="shrink-0">
                <i class="i-heroicons-document-check text-3xl text-success-600" aria-hidden="true" />
              </div>
              <div class="flex-1">
                <h3 class="text-sm font-medium text-gray-900 mb-2">Convention signée</h3>
                <p class="text-sm text-gray-600 mb-4">
                  Téléchargez la convention de partenariat signée par le sponsor (PDF, max 10MB).
                </p>

                <div v-if="signedAgreementError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ signedAgreementError }}
                </div>

                <div v-if="signedAgreementSuccess" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4 text-sm">
                  {{ signedAgreementSuccess }}
                </div>

                <div v-if="signedAgreementUrl" class="mb-4">
                  <div class="flex items-center gap-3 p-3 bg-green-50 border border-green-200 rounded">
                    <i class="i-heroicons-document-check text-2xl text-success-600" aria-hidden="true" />
                    <div class="flex-1">
                      <p class="text-sm font-medium text-gray-900">Convention signée téléchargée</p>
                      <a
                        :href="signedAgreementUrl"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="inline-flex items-center gap-1 text-sm text-primary-600 hover:text-primary-800"
                      >
                        Télécharger la convention signée
                        <i class="i-heroicons-arrow-down-tray" aria-hidden="true" />
                      </a>
                    </div>
                  </div>
                </div>

                <div class="flex gap-3 items-center">
                  <input
                    ref="signedAgreementFileInput"
                    type="file"
                    accept="application/pdf"
                    class="hidden"
                    @change="handleSignedAgreementFileChange"
                  />
                  <UButton
                    color="success"
                    variant="outline"
                    :loading="isUploadingSignedAgreement"
                    @click="() => signedAgreementFileInput?.click()"
                  >
                    <i class="i-heroicons-arrow-up-tray mr-2" aria-hidden="true" />
                    {{ signedAgreementUrl ? 'Remplacer la convention signée' : 'Télécharger la convention signée' }}
                  </UButton>

                  <span v-if="selectedSignedAgreementFile" class="text-sm text-gray-600">
                    {{ selectedSignedAgreementFile.name }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getEventsPartnershipDetailed, postCompaniesLogo, postEventsPartnershipSignedAgreement } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type { ExtendedPartnershipItem } from "~/types/partnership";

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

const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const partnership = ref<ExtendedPartnershipItem | null>(null);
const companyId = ref<string | null>(null);
const companyLogo = ref<string | null>(null);
const signedAgreementUrl = ref<string | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

// États pour les documents
const isGeneratingAgreement = ref(false);
const agreementUrl = ref<string | null>(null);
const agreementError = ref<string | null>(null);

const isGeneratingQuote = ref(false);
const quoteUrl = ref<string | null>(null);
const quoteError = ref<string | null>(null);

const isGeneratingInvoice = ref(false);
const invoiceUrl = ref<string | null>(null);
const invoiceError = ref<string | null>(null);

// États pour l'upload du logo
const logoFileInput = ref<HTMLInputElement | null>(null);
const selectedLogoFile = ref<File | null>(null);
const isUploadingLogo = ref(false);
const logoError = ref<string | null>(null);
const logoSuccess = ref<string | null>(null);

// États pour l'upload de la convention signée
const signedAgreementFileInput = ref<HTMLInputElement | null>(null);
const selectedSignedAgreementFile = ref<File | null>(null);
const isUploadingSignedAgreement = ref(false);
const signedAgreementError = ref<string | null>(null);
const signedAgreementSuccess = ref<string | null>(null);

// Menu contextuel pour la page du sponsor
const { sponsorLinks } = useSponsorLinks(orgSlug.value, eventSlug.value, sponsorId.value);

async function loadPartnership() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getEventsPartnershipDetailed(eventSlug.value, sponsorId.value);
    const { partnership: p, company } = response.data;

    // Stocker l'ID de la company pour l'upload du logo
    companyId.value = company.id;

    // Stocker l'URL du logo si elle existe
    companyLogo.value = company.medias?.original || null;

    // Stocker l'URL de la convention signée si elle existe
    signedAgreementUrl.value = p.process_status?.agreement_signed_url || null;

    // Créer l'objet partnership simplifié pour l'affichage
    partnership.value = {
      id: p.id,
      company_name: company.name,
      event_name: '',
      contact: {
        display_name: p.contact_name,
        role: p.contact_role
      }
    } as ExtendedPartnershipItem;
  } catch (err) {
    console.error('Failed to load partnership:', err);
    error.value = 'Impossible de charger les informations du sponsor';
  } finally {
    loading.value = false;
  }
}

/**
 * Génère la convention de partenariat
 */
async function handleGenerateAgreement() {
  if (!partnership.value || !orgSlug.value || !eventSlug.value || !sponsorId.value) return;

  const { generateDocument } = useDocumentGeneration();

  await generateDocument(
    async () => {
      const { postOrgsEventsPartnershipAgreement } = await import('~/utils/api');
      return await postOrgsEventsPartnershipAgreement(
        orgSlug.value,
        eventSlug.value,
        sponsorId.value
      );
    },
    'agreement',
    isGeneratingAgreement,
    agreementError,
    agreementUrl
  );
}

/**
 * Génère le devis
 */
async function handleGenerateQuote() {
  if (!partnership.value || !eventSlug.value || !sponsorId.value) return;

  const { generateDocument } = useDocumentGeneration();

  await generateDocument(
    async () => {
      const { postEventsPartnershipBillingQuote } = await import('~/utils/api');
      return await postEventsPartnershipBillingQuote(
        eventSlug.value,
        sponsorId.value
      );
    },
    'quote',
    isGeneratingQuote,
    quoteError,
    quoteUrl
  );
}

/**
 * Génère la facture
 */
async function handleGenerateInvoice() {
  if (!partnership.value || !eventSlug.value || !sponsorId.value) return;

  const { generateDocument } = useDocumentGeneration();

  await generateDocument(
    async () => {
      const { postEventsPartnershipBillingInvoice } = await import('~/utils/api');
      return await postEventsPartnershipBillingInvoice(
        eventSlug.value,
        sponsorId.value
      );
    },
    'invoice',
    isGeneratingInvoice,
    invoiceError,
    invoiceUrl
  );
}

/**
 * Gère le changement de fichier pour le logo
 */
function handleLogoFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  // Vérifier le type de fichier
  if (!['image/png', 'image/jpeg', 'image/jpg'].includes(file.type)) {
    logoError.value = 'Le fichier doit être au format PNG ou JPG';
    return;
  }

  // Vérifier la taille (5MB max)
  if (file.size > 5 * 1024 * 1024) {
    logoError.value = 'Le fichier ne doit pas dépasser 5MB';
    return;
  }

  selectedLogoFile.value = file;
  uploadLogo(file);
}

/**
 * Upload le logo de l'entreprise
 */
async function uploadLogo(file: File) {
  if (!companyId.value || !eventSlug.value || !sponsorId.value) {
    logoError.value = 'ID de l\'entreprise non disponible';
    return;
  }

  isUploadingLogo.value = true;
  logoError.value = null;
  logoSuccess.value = null;

  try {
    await postCompaniesLogo(companyId.value, { file });

    logoSuccess.value = 'Logo téléchargé avec succès';
    const toast = useCustomToast();
    toast.success('Le logo a été téléchargé avec succès');

    // Recharger les données pour afficher le logo
    await loadPartnership();

    // Réinitialiser l'input file
    if (logoFileInput.value) {
      logoFileInput.value.value = '';
    }
    selectedLogoFile.value = null;

    // Effacer le message de succès après 3 secondes
    setTimeout(() => {
      logoSuccess.value = null;
    }, 3000);
  } catch (err: any) {
    console.error('Failed to upload logo:', err);
    logoError.value = err.response?.data?.message || 'Impossible de télécharger le logo';

    const toast = useCustomToast();
    toast.error(logoError.value);
  } finally {
    isUploadingLogo.value = false;
  }
}

/**
 * Gère le changement de fichier pour la convention signée
 */
function handleSignedAgreementFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  // Vérifier le type de fichier
  if (file.type !== 'application/pdf') {
    signedAgreementError.value = 'Le fichier doit être au format PDF';
    return;
  }

  // Vérifier la taille (10MB max)
  if (file.size > 10 * 1024 * 1024) {
    signedAgreementError.value = 'Le fichier ne doit pas dépasser 10MB';
    return;
  }

  selectedSignedAgreementFile.value = file;
  uploadSignedAgreement(file);
}

/**
 * Upload la convention signée
 */
async function uploadSignedAgreement(file: File) {
  if (!eventSlug.value || !sponsorId.value) {
    signedAgreementError.value = 'Informations manquantes';
    return;
  }

  isUploadingSignedAgreement.value = true;
  signedAgreementError.value = null;
  signedAgreementSuccess.value = null;

  try {
    await postEventsPartnershipSignedAgreement(eventSlug.value, sponsorId.value, { file });

    signedAgreementSuccess.value = 'Convention signée téléchargée avec succès';
    const toast = useCustomToast();
    toast.success('La convention signée a été téléchargée avec succès');

    // Recharger les données pour afficher la convention signée
    await loadPartnership();

    // Réinitialiser l'input file
    if (signedAgreementFileInput.value) {
      signedAgreementFileInput.value.value = '';
    }
    selectedSignedAgreementFile.value = null;

    // Effacer le message de succès après 3 secondes
    setTimeout(() => {
      signedAgreementSuccess.value = null;
    }, 3000);
  } catch (err: any) {
    console.error('Failed to upload signed agreement:', err);
    signedAgreementError.value = err.response?.data?.message || 'Impossible de télécharger la convention signée';

    const toast = useCustomToast();
    toast.error(signedAgreementError.value);
  } finally {
    isUploadingSignedAgreement.value = false;
  }
}

onMounted(() => {
  loadPartnership();
});

watch([orgSlug, eventSlug, sponsorId], () => {
  loadPartnership();
});

useHead({
  title: computed(() => `Documents - ${partnership.value?.company_name || 'Sponsor'} | DevLille`)
});
</script>
