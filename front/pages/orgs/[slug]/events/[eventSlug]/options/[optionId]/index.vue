<template>
  <div v-if="option">
    <h2>{{ option.name }}</h2>
    
    <div class="option-details">
      <dl>
        <dt>Prix</dt>
        <dd>{{ option.price ? `${option.price} €` : 'Gratuit' }}</dd>
        
        <dt>Description</dt>
        <dd>{{ option.description || 'Aucune description' }}</dd>
      </dl>
      
      <div class="actions">
        <NuxtLink :to="`/orgs/${route.params.slug}/events/${route.params.eventSlug}/options/${route.params.optionId}/settings`">
          Paramètres
        </NuxtLink>
      </div>
    </div>
  </div>
  
  <div v-else-if="loading">
    Chargement...
  </div>
  
  <div v-else>
    Option de sponsoring non trouvée
  </div>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugOptions } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const option = ref<any>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugOptions(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    
    // Find the specific option by ID
    option.value = response.data.find(o => o.id === route.params.optionId) || null;
  } catch (error) {
    console.error('Failed to load sponsoring option:', error);
  } finally {
    loading.value = false;
  }
});

useHead({
  title: () => option.value ? `${option.value.name} | Options de Sponsoring` : 'Option de Sponsoring'
});
</script>

<style scoped>
.option-details {
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
</style>