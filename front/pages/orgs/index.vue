<template>
  <Dashboard :main-links="mainLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900">Liste des Organisations</h1>
        <UButton
          to="/orgs/create"
          label="Créer une organisation"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <UTable :data="data" />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getUsersMeOrgs, type OrganisationItem } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

const { mainLinks, footerLinks } = useDashboardLinks()

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const data = ref<OrganisationItem[]>([]);

onMounted(async () => {
  try {
    const response = await getUsersMeOrgs();
    data.value = response.data;
  } catch (error) {
    console.error('Failed to load organizations:', error);
  }
});

useHead({
  title: "Liste de Organisations | DevLille"
});
</script>
