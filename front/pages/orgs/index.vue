<template>
  <h2>Liste de Organisations</h2>

  <div if="data.length > 0" class="results-list grid">
    <div v-for="evt in data" :key="evt.slug" class="card">
      <h3>
        <NuxtLink
          :to="{ name: 'orgs-slug-settings', params: { slug: evt.slug } }"
          >{{ evt.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>Owner</dt>
        <dd>{{ evt.owner?.display_name }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink to="/orgs/create">Créer une nouvelle organisation</NuxtLink>
</template>

<script setup lang="ts">
import { getUsersMeOrgs, type OrganisationItem } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

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
