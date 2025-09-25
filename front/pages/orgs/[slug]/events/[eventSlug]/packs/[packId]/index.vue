<template>
  <div v-if="pack">
    <h2>{{ pack.name }}</h2>
    
    <div class="pack-details">
      <dl>
        <dt>Prix de base</dt>
        <dd>{{ pack.basePrice }} €</dd>
        
        <dt>Quantité maximale</dt>
        <dd>{{ pack.maxQuantity || 'Illimitée' }}</dd>
        
        <dt>Options requises</dt>
        <dd v-if="pack.requiredOptions.length > 0">
          <ul>
            <li v-for="option in pack.requiredOptions" :key="option.id">
              {{ option.name }}
              <span v-if="option.price"> - {{ option.price }} €</span>
              <p v-if="option.description">{{ option.description }}</p>
            </li>
          </ul>
        </dd>
        <dd v-else>Aucune option requise</dd>
        
        <dt>Options facultatives</dt>
        <dd v-if="pack.optionalOptions.length > 0">
          <ul>
            <li v-for="option in pack.optionalOptions" :key="option.id">
              {{ option.name }}
              <span v-if="option.price"> - {{ option.price }} €</span>
              <p v-if="option.description">{{ option.description }}</p>
            </li>
          </ul>
        </dd>
        <dd v-else>Aucune option facultative</dd>
      </dl>
      
      <div class="actions">
        <NuxtLink :to="`/orgs/${route.params.slug}/events/${route.params.eventSlug}/packs/${route.params.packId}/settings`">
          Paramètres
        </NuxtLink>
      </div>
    </div>
  </div>
  
  <div v-else-if="loading">
    Chargement...
  </div>
  
  <div v-else>
    Pack de sponsoring non trouvé
  </div>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugPacks, type SponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const pack = ref<SponsoringPack | null>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugPacks(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    
    // Find the specific pack by ID
    pack.value = response.data.find(p => p.id === route.params.packId) || null;
  } catch (error) {
    console.error('Failed to load sponsoring pack:', error);
  } finally {
    loading.value = false;
  }
});

useHead({
  title: () => pack.value ? `${pack.value.name} | Packs de Sponsoring` : 'Pack de Sponsoring'
});
</script>

<style scoped>
.pack-details {
  max-width: 800px;
}

.actions {
  margin-top: 2rem;
}

.actions a {
  display: inline-block;
  padding: 0.5rem 1rem;
  background-color: #007bff;
  color: white;
  text-decoration: none;
  border-radius: 4px;
}

.actions a:hover {
  background-color: #0056b3;
}

ul {
  list-style-type: disc;
  padding-left: 1.5rem;
}

ul li {
  margin-bottom: 0.5rem;
}

ul li p {
  margin: 0.25rem 0 0 0;
  color: #666;
  font-size: 0.9rem;
}
</style>