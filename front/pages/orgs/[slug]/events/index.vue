<template>
  <h2>Liste de Evenements</h2>

  <div class="results-list grid">
    <div v-for="evt in data" :key="evt.slug" class="card">
      <h3>
        <NuxtLink
          :to="{
            name: 'orgs-slug-events-id-settings',
            params: { slug: route.params.slug, id: evt.slug },
          }"
          >{{ evt.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>start Date</dt>
        <dd>{{ evt.start_time }}</dd>
        <dt>end Date</dt>
        <dd>{{ evt.end_time }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink :to="`/orgs/${route.params.slug}/events/create`">Créer une nouvelle organisation</NuxtLink>

</template>

<script setup lang="ts">
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();

const data = await getOrgsOrgSlugEvents(route.params.slug as string).then(
  (r) => r.data
);

useHead({
  title: "Liste de Organisations | DevLille",
});
</script>
