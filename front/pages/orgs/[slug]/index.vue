<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <BackButton to="/orgs" label="Retour" />
          <PageTitle>{{ organisation?.name }}</PageTitle>
        </div>
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <OrganisationForm
        v-else-if="organisation"
        :data="organisation"
        @save="onSave"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgs, putOrgs, type Organisation } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const slug = computed(() => route.params.slug as string);
const organisation = ref<Organisation | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const { orgLinks } = useOrgLinks(slug.value);

async function loadOrganisation() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getOrgs(slug.value);
    organisation.value = response.data;
  } catch (err) {
    console.error('Failed to load organization:', err);
    error.value = 'Impossible de charger les informations de l\'organisation';
  } finally {
    loading.value = false;
  }
}

async function onSave(updatedOrg: Organisation) {
  try {
    await putOrgs(slug.value, updatedOrg);
    organisation.value = updatedOrg;
    // Afficher un message de succès (vous pouvez ajouter un toast ici)
    console.log('Organisation mise à jour avec succès');
  } catch (err) {
    console.error('Failed to update organization:', err);
    error.value = 'Impossible de mettre à jour l\'organisation';
  }
}

onMounted(() => {
  loadOrganisation();
});

// Recharger si le slug change
watch(slug, () => {
  loadOrganisation();
});

useHead({
  title: computed(() => `${organisation.value?.name || 'Organisation'} | DevLille`)
});
</script>
