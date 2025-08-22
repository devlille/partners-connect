<template>
  <h2>Liste de Organisations</h2>

  <div class="results-list grid">
    <div v-for="evt in data" :key="evt.id" class="card">
      <h3>
        <NuxtLink
          :to="{ name: 'orgs-slug-settings', params: { slug: evt.slug } }"
          >{{ evt.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>Owner</dt>
        <dd>{{ evt.owner.display_name }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink to="/orgs/create">Créer une nouvelle organisation</NuxtLink>
</template>

<script setup lang="ts">
import { getUsersMeOrgs } from "~/utils/api";

const data = await getUsersMeOrgs().then((r) => r.data);

useHead({
  title: "Liste de Organisations | DevLille"
});
</script>
