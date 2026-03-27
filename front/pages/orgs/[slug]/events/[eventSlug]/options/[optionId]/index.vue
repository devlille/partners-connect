<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}/options`" label="Retour" />
          <PageTitle>{{ optionDetail ? getOptionName(optionDetail.option) : 'Option' }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else-if="optionDetail">
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">Informations de l'option</h2>
          <SponsoringOptionForm :data="optionDetail.option" @save="onSave" />
        </div>

        <div class="bg-white rounded-lg shadow p-6">
          <div class="flex items-center gap-3 mb-4">
            <h2 class="text-lg font-semibold text-gray-900">Partenariats utilisant cette option</h2>
            <UBadge
              :color="optionDetail.partnerships.length > 0 ? 'primary' : 'neutral'"
              variant="subtle"
            >
              {{ optionDetail.partnerships.length }}
            </UBadge>
          </div>

          <div v-if="optionDetail.partnerships.length === 0" class="text-sm text-gray-500">
            Aucun partenariat validé n'utilise cette option.
          </div>

          <table v-else class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Entreprise
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Contact
                </th>
                <th
                  class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  Pack validé
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr
                v-for="partnership in optionDetail.partnerships"
                :key="partnership.id"
                class="hover:bg-gray-50 cursor-pointer"
                @click="navigateTo(`/orgs/${orgSlug}/events/${eventSlug}/sponsors/${partnership.id}`)"
              >
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ partnership.company_name }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ partnership.contact.first_name }} {{ partnership.contact.last_name }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ partnership.validated_pack_id ? (partnership.selected_pack_name || partnership.validated_pack_id) : '—' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEventsOptionsById, putOrgsEventsOptions, type SponsoringOptionWithPartnershipsSchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter();
const { footerLinks } = useDashboardLinks();
const { getOptionName } = useOptionTranslation();

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

const optionId = computed(() => {
  const params = route.params.optionId;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const optionDetail = ref<SponsoringOptionWithPartnershipsSchema | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

async function loadOption() {
  try {
    loading.value = true;
    error.value = null;

    const response = await getOrgsEventsOptionsById(orgSlug.value, eventSlug.value, optionId.value);
    optionDetail.value = response.data;
  } catch (err) {
    console.error('Failed to load option:', err);
    error.value = 'Impossible de charger l\'option';
  } finally {
    loading.value = false;
  }
}

async function onSave(data: any) {
  try {
    error.value = null;

    const optionData = data.option;

    await putOrgsEventsOptions(orgSlug.value, eventSlug.value, optionId.value, optionData);

    router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/options`);
  } catch (err) {
    console.error('Failed to update option:', err);
    error.value = 'Impossible de mettre à jour l\'option';
  }
}

onMounted(() => {
  loadOption();
});

watch([orgSlug, eventSlug, optionId], () => {
  loadOption();
});

useHead({
  title: computed(() => `${optionDetail.value ? getOptionName(optionDetail.value.option) : 'Option'} | DevLille`)
});
</script>
