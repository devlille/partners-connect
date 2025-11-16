<template>
  <Dashboard :main-links="mainLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <PageTitle>Liste des Organisations</PageTitle>
        <UButton
          to="/orgs/create"
          label="Créer une organisation"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <UTable
        :data="data"
        :columns="columns"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getUsersMeOrgs, type OrganisationItem } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type {TableRow} from "@nuxt/ui";

const { mainLinks, footerLinks } = useDashboardLinks()

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const data = ref<OrganisationItem[]>([]);

const columns = [
  {
    header: 'Nom',
    accessorKey: 'name',
    cell: (info: TableRow<OrganisationItem>) => {
      const org = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${org.slug}`),
        class: 'cursor-pointer hover:underline'
      }, info.getValue('name'));
    }
  },
  {
    header: 'Siège social',
    accessorKey: 'head_office',
    cell: (info: TableRow<OrganisationItem>) => {
      const org = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${org.slug}`),
        class: 'cursor-pointer'
      }, info.getValue('head_office'));
    }
  }
];

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
