<template>
  <h2>Liste des Packs de Sponsoring</h2>

  <div v-if="data.length > 0" class="results-list grid">
    <div v-for="pack in data" :key="pack.id" class="card">
      <h3>
        <NuxtLink
          :to="{ name: 'orgs-slug-events-eventSlug-packs-packId', params: { slug: route.params.slug, eventSlug: route.params.eventSlug, packId: pack.id } }"
          >{{ pack.name }}</NuxtLink
        >
      </h3>

      <dl>
        <dt>Prix de base</dt>
        <dd>{{ pack.basePrice }} €</dd>
        <dt>Quantité maximale</dt>
        <dd>{{ pack.maxQuantity || 'Illimitée' }}</dd>
      </dl>
    </div>
  </div>
  <NuxtLink :to="`/orgs/${route.params.slug}/events/${route.params.eventSlug}/packs/create`">
    Créer un nouveau pack de sponsoring
  </NuxtLink>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugPacks, type GetOrgsOrgSlugEventsEventSlugPacksResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const data = ref<GetOrgsOrgSlugEventsEventSlugPacksResult['data']>([]);

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugPacks(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    data.value = response.data;
  } catch (error) {
    console.error('Failed to load sponsoring packs:', error);
  } 
});

useHead({
  title: `Packs de Sponsoring | ${route.params.eventSlug}`
});
</script>