<template>
  <h2>Liste des Événements</h2>

  <div v-if="data.length > 0" class="results-list grid">
    <div v-for="evt in data" :key="evt.slug" class="card">
      <h3>
        <NuxtLink
          :to="{ name: 'orgs-slug-events-eventSlug', params: { slug: route.params.slug, eventSlug: evt.slug } }"
          >{{ evt.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>Date de début</dt>
        <dd>{{ evt.start_time }}</dd>
        <dt>Date de fin</dt>
        <dd>{{ evt.end_time }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink :to="`/orgs/${route.params.slug}/events/create`">Créer un nouvel événement</NuxtLink>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEvents, type GetOrgsOrgSlugEventsResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const data = ref<GetOrgsOrgSlugEventsResult['data']['items']>([]);

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEvents(route.params.slug as string);
    data.value = response.data.items;
  } catch (error) {
    console.error('Failed to load events:', error);
  } 
});

useHead({
  title: `Liste des Événements | ${route.params.slug}`
});
</script>