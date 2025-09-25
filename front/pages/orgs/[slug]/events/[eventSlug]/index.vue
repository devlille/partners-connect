<template>
  <div v-if="event">
    <h2>{{ event.event.name }}</h2>
    
    
    <!-- Section des Partnerships -->
    <div class="partnerships-section">
      <h3>Partenaires & Sponsors</h3>
      
      <div v-if="partnerships.length > 0" class="results-list grid">
        <div v-for="partnership in partnerships" :key="partnership.id" class="card">
          <h4>{{ partnership.sponsor?.name || 'Sponsor sans nom' }}</h4>
          
          <dl>
            <dt>Pack</dt>
            <dd>{{ partnership.pack?.name || 'Non défini' }}</dd>
            
            <dt>Statut</dt>
            <dd>{{ partnership.status }}</dd>
            
            <dt>Prix total</dt>
            <dd>{{ partnership.total_price }} €</dd>
          </dl>
        </div>
      </div>
      
      <div v-else-if="!loadingPartnerships">
        <p>Aucun partenaire ou sponsor pour cet événement.</p>
      </div>
      
      <div v-if="loadingPartnerships">
        Chargement des partenaires...
      </div>
    </div>
  </div>
  
  <div v-else-if="loading">
    Chargement...
  </div>
  
  <div v-else>
    Événement non trouvé
  </div>
</template>

<script setup lang="ts">
import { getEventsEventSlug, getUsersMeOrgs, getOrgsOrgSlugEventsEventSlugPartnership, type GetEventsEventSlugResult, type OrganisationItem, type GetOrgsOrgSlugEventsEventSlugPartnershipResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const event = ref<GetEventsEventSlugResult['data'] | null>(null);
const organisation = ref<OrganisationItem | null>(null);
const partnerships = ref<GetOrgsOrgSlugEventsEventSlugPartnershipResult['data']['items']>([]);
const loading = ref(true);
const loadingPartnerships = ref(true);

onMounted(async () => {
  try {
    // Récupérer les détails de l'événement
    const eventResponse = await getEventsEventSlug(route.params.eventSlug as string);
    event.value = eventResponse.data;
    
    // Récupérer les informations de l'organisation
    const orgsResponse = await getUsersMeOrgs();
    organisation.value = orgsResponse.data.find(org => org.slug === route.params.slug) || null;
    
  } catch (error) {
    console.error('Failed to load event:', error);
  } finally {
    loading.value = false;
  }

  // Récupérer les partnerships en parallèle
  try {
    const partnershipResponse = await getOrgsOrgSlugEventsEventSlugPartnership(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    partnerships.value = partnershipResponse.data.items;
  } catch (error) {
    console.error('Failed to load partnerships:', error);
  } finally {
    loadingPartnerships.value = false;
  }
});

useHead({
  title: () => event.value ? `${event.value.event.name} | ${route.params.slug}` : 'Événement'
});
</script>

<style scoped>
.event-details {
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

.actions a {
  margin-right: 1rem;
}

.partnerships-section {
  margin-top: 3rem;
}

.partnerships-section h3 {
  margin-bottom: 1.5rem;
  color: #333;
}

.results-list.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1rem;
  background-color: #f9f9f9;
}

.card h4 {
  margin: 0 0 1rem 0;
  color: #007bff;
}

.card dl {
  margin: 0;
}

.card dt {
  font-weight: bold;
  margin-top: 0.5rem;
}

.card dd {
  margin: 0 0 0.5rem 0;
}
</style>