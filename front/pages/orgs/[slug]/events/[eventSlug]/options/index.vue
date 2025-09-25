<template>
  <h2>Liste des Options de Sponsoring</h2>

  <div v-if="data.length > 0" class="results-list grid">
    <div v-for="option in data" :key="option.id" class="card">
      <h3>
        <NuxtLink
          :to="{ name: 'orgs-slug-events-eventSlug-options-optionId', params: { slug: route.params.slug, eventSlug: route.params.eventSlug, optionId: option.id } }"
          >{{ option.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>Prix</dt>
        <dd>{{ option.price ? `${option.price} €` : 'Gratuit' }}</dd>
        <dt>Description</dt>
        <dd>{{ option.description || 'Aucune description' }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink :to="`/orgs/${route.params.slug}/events/${route.params.eventSlug}/options/create`">
    Créer une nouvelle option de sponsoring
  </NuxtLink>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugOptions, type GetOrgsOrgSlugEventsEventSlugOptionsResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const data = ref<GetOrgsOrgSlugEventsEventSlugOptionsResult['data']>([]);

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugOptions(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    data.value = response.data;
  } catch (error) {
    console.error('Failed to load sponsoring options:', error);
  } 
});

useHead({
  title: `Options de Sponsoring | ${route.params.eventSlug}`
});
</script>