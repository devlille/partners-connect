<template>
  <h2>Liste de Organisations</h2>

  <div class="results-list grid">
    <div class="card" v-for="evt in data" :key="evt.id">
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
</template>

<script setup lang="ts">
const sdk = useSdk();
const data = await sdk.getUserOrgs();

useHead({
  title: "Liste de Organisations | DevLille",
  script: [
    {
      src: "/js/main-nav.js",
      type: "text/javascript",
      defer: true,
    },
    {
      src: "/js/autoScroll.js",
      type: "text/javascript",
      defer: true,
    },
    {
      src: "/js/tabs.js",
      type: "text/javascript",
      defer: true,
    },
  ],
});
</script>
