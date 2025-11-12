<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${orgSlug}/events/${eventSlug}`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <PageTitle>Intégrations - {{ eventSlug }}</PageTitle>
          <p class="text-gray-600 mt-1">Configurez les intégrations tierces pour cet événement</p>
        </div>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <!-- Qonto Integration -->
      <div class="bg-white border border-gray-200 rounded-lg p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <i class="i-heroicons-banknotes text-2xl text-purple-600" />
            </div>
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Qonto</h2>
              <p class="text-sm text-gray-600">Gestion de la facturation</p>
            </div>
          </div>
        </div>

        <form @submit.prevent="saveQontoIntegration" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Organization Slug
            </label>
            <UInput
              v-model="qontoConfig.organization_slug"
              placeholder="mon-organisation"
              :disabled="savingQonto"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Secret Key
            </label>
            <UInput
              v-model="qontoConfig.secret_key"
              type="password"
              placeholder="sk_live_..."
              :disabled="savingQonto"
            />
          </div>

          <div v-if="qontoError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {{ qontoError }}
          </div>

          <UButton
            type="submit"
            :loading="savingQonto"
            :disabled="!qontoConfig.organization_slug || !qontoConfig.secret_key"
            label="Enregistrer Qonto"
            icon="i-heroicons-check"
          />
        </form>
      </div>

      <!-- Mailjet Integration -->
      <div class="bg-white border border-gray-200 rounded-lg p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <i class="i-heroicons-paper-airplane text-2xl text-blue-600" />
            </div>
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Mailjet</h2>
              <p class="text-sm text-gray-600">Service d'envoi d'emails</p>
            </div>
          </div>
        </div>

        <form @submit.prevent="saveMailjetIntegration" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              API Key
            </label>
            <UInput
              v-model="mailjetConfig.api_key"
              placeholder="votre-api-key"
              :disabled="savingMailjet"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              API Secret
            </label>
            <UInput
              v-model="mailjetConfig.api_secret"
              type="password"
              placeholder="votre-api-secret"
              :disabled="savingMailjet"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Email expéditeur
            </label>
            <UInput
              v-model="mailjetConfig.from_email"
              type="email"
              placeholder="contact@exemple.com"
              :disabled="savingMailjet"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Nom expéditeur
            </label>
            <UInput
              v-model="mailjetConfig.from_name"
              placeholder="Mon Événement"
              :disabled="savingMailjet"
            />
          </div>

          <div v-if="mailjetError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {{ mailjetError }}
          </div>

          <UButton
            type="submit"
            :loading="savingMailjet"
            :disabled="!mailjetConfig.api_key || !mailjetConfig.api_secret || !mailjetConfig.from_email || !mailjetConfig.from_name"
            label="Enregistrer Mailjet"
            icon="i-heroicons-check"
          />
        </form>
      </div>

      <!-- Billetweb Integration -->
      <div class="bg-white border border-gray-200 rounded-lg p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <i class="i-heroicons-ticket text-2xl text-green-600" />
            </div>
            <div>
              <h2 class="text-xl font-semibold text-gray-900">Billetweb</h2>
              <p class="text-sm text-gray-600">Gestion de la billetterie</p>
            </div>
          </div>
        </div>

        <form @submit.prevent="saveBilletwebIntegration" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              API Key
            </label>
            <UInput
              v-model="billetwebConfig.api_key"
              placeholder="votre-api-key"
              :disabled="savingBilletweb"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Event ID
            </label>
            <UInput
              v-model="billetwebConfig.event_id"
              placeholder="12345"
              :disabled="savingBilletweb"
            />
          </div>

          <div v-if="billetwebError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {{ billetwebError }}
          </div>

          <UButton
            type="submit"
            :loading="savingBilletweb"
            :disabled="!billetwebConfig.api_key || !billetwebConfig.event_id"
            label="Enregistrer Billetweb"
            icon="i-heroicons-check"
          />
        </form>
      </div>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { postOrgsEventsIntegrations } from "~/utils/api";
import type { QontoConfig, MailjetConfig, BilletwebConfig } from "~/types/integration";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const toast = useCustomToast();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

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

// Qonto state
const qontoConfig = ref<QontoConfig>({
  organization_slug: '',
  secret_key: ''
});
const savingQonto = ref(false);
const qontoError = ref<string | null>(null);

// Mailjet state
const mailjetConfig = ref<MailjetConfig>({
  api_key: '',
  api_secret: '',
  from_email: '',
  from_name: ''
});
const savingMailjet = ref(false);
const mailjetError = ref<string | null>(null);

// Billetweb state
const billetwebConfig = ref<BilletwebConfig>({
  api_key: '',
  event_id: ''
});
const savingBilletweb = ref(false);
const billetwebError = ref<string | null>(null);

async function saveQontoIntegration() {
  try {
    savingQonto.value = true;
    qontoError.value = null;

    await postOrgsEventsIntegrations(
      orgSlug.value,
      eventSlug.value,
      'QONTO',
      'BILLING',
      qontoConfig.value
    );

    toast.success('Intégration Qonto enregistrée avec succès');
  } catch (err: any) {
    console.error('Failed to save Qonto integration:', err);
    if (err.response?.data?.message) {
      qontoError.value = err.response.data.message;
    } else {
      qontoError.value = 'Impossible d\'enregistrer l\'intégration Qonto';
    }
    toast.error(qontoError.value);
  } finally {
    savingQonto.value = false;
  }
}

async function saveMailjetIntegration() {
  try {
    savingMailjet.value = true;
    mailjetError.value = null;

    await postOrgsEventsIntegrations(
      orgSlug.value,
      eventSlug.value,
      'MAILJET',
      'MAILING',
      mailjetConfig.value
    );

    toast.success('Intégration Mailjet enregistrée avec succès');
  } catch (err: any) {
    console.error('Failed to save Mailjet integration:', err);
    if (err.response?.data?.message) {
      mailjetError.value = err.response.data.message;
    } else {
      mailjetError.value = 'Impossible d\'enregistrer l\'intégration Mailjet';
    }
    toast.error(mailjetError.value);
  } finally {
    savingMailjet.value = false;
  }
}

async function saveBilletwebIntegration() {
  try {
    savingBilletweb.value = true;
    billetwebError.value = null;

    await postOrgsEventsIntegrations(
      orgSlug.value,
      eventSlug.value,
      'BILLETWEB',
      'TICKETING',
      billetwebConfig.value
    );

    toast.success('Intégration Billetweb enregistrée avec succès');
  } catch (err: any) {
    console.error('Failed to save Billetweb integration:', err);
    if (err.response?.data?.message) {
      billetwebError.value = err.response.data.message;
    } else {
      billetwebError.value = 'Impossible d\'enregistrer l\'intégration Billetweb';
    }
    toast.error(billetwebError.value);
  } finally {
    savingBilletweb.value = false;
  }
}

useHead({
  title: `Intégrations - ${eventSlug.value} | DevLille`
});
</script>
