<template>
  <div v-if="event">
    <h2>{{ event.event.name }}</h2>
    
    
    <!-- Section des Partnerships -->
    <div class="partnerships-section">
      <h3>Partenaires & Sponsors</h3>

      <div v-if="loadingPartnerships">
        Chargement des partenaires...
      </div>

      <UTable
        v-else-if="partnerships.length > 0"
        :data="tableRows"
      />

      <div v-else>
        <p>Aucun partenaire ou sponsor pour cet événement.</p>
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
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const event = ref<GetEventBySlugResult['data'] | null>(null);
const organisation = ref<OrganisationItem | null>(null);
const partnerships = ref<PartnershipItemSchema[]>([]);
const packs = ref<SponsoringPackSchema[]>([]);
const loading = ref(true);
const loadingPartnerships = ref(true);

const columns = [
  { key: 'company_name', label: 'Compagnie' },
  { key: 'pack_name', label: 'Pack' },
  { key: 'pack_price', label: 'Prix du pack' }
];

onMounted(async () => {
  try {
    // Récupérer les détails de l'événement
    const eventResponse = await getEventBySlug(route.params.eventSlug as string);
    event.value = eventResponse.data;
    
    // Récupérer les informations de l'organisation
    const orgsResponse = await getUsersMeOrgs();
    organisation.value = orgsResponse.data.find(org => org.slug === route.params.slug) || null;
    
  } catch (error) {
    console.error('Failed to load event:', error);
  } finally {
    loading.value = false;
  }

  // Récupérer les partnerships et packs en parallèle
  try {
    const [partnershipResponse, packsResponse] = await Promise.all([
      getOrgsEventsPartnership(
        route.params.slug as string,
        route.params.eventSlug as string
      ),
      getOrgsEventsPacks(
        route.params.slug as string,
        route.params.eventSlug as string
      )
    ]);

    partnerships.value = partnershipResponse.data;
    packs.value = packsResponse.data;
    console.log('Partnerships loaded:', partnerships.value.length);
    console.log('Partnerships data:', partnerships.value);
    console.log('Packs loaded:', packs.value.length);
  } catch (error) {
    console.error('Failed to load partnerships or packs:', error);
  } finally {
    loadingPartnerships.value = false;
    console.log('Loading partnerships finished:', loadingPartnerships.value);
  }
});

const getPackPrice = (packName: string | null | undefined) => {
  if (!packName) return 0;
  const pack = packs.value.find(p => p.name === packName);
  return pack?.base_price ?? 0;
};

const tableRows = computed(() => {
  const rows = partnerships.value.map(partnership => ({
    company_name: partnership.company_name || 'Sponsor sans nom',
    pack_name: partnership.pack_name || 'Non défini',
    pack_price: `${getPackPrice(partnership.pack_name)} €`
  }));
  console.log('Table rows:', rows);
  return rows;
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